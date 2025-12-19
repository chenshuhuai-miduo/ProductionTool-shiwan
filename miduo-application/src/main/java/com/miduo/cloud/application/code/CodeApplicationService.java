package com.miduo.cloud.application.code;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.code.*;
import com.miduo.cloud.infrastructure.persistence.mybatis.mapper.CodeRelationMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.mapper.ProductionOrderDetailMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.mapper.ProductionOrderMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.ProductionOrderDetailPO;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.ProductionOrderPO;
import com.miduo.cloud.application.log.OperateLogApplicationService;
import com.miduo.cloud.entity.po.OperateLog;
import com.miduo.cloud.entity.enums.ModuleNameEnum;
import com.miduo.cloud.entity.enums.OperateTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 码管理应用服务
 * 完全按照原始业务逻辑实现（CodeServiceImpl.java）
 */
@Service
public class CodeApplicationService {
    
    @Autowired
    private CodeRelationMapper codeRelationMapper;
    
    @Autowired
    private ProductionOrderDetailMapper productionOrderDetailMapper;
    
    @Autowired
    private ProductionOrderMapper productionOrderMapper;
    
    @Autowired
    private com.miduo.cloud.infrastructure.persistence.mybatis.mapper.CodeRelationMapper codeRelationUploadMapper;
    
    @Autowired
    private OperateLogApplicationService operateLogApplicationService;
    
    @Autowired
    private CodeBloomFilterManager bloomFilterManager;
    
    /**
     * 标签编号缓存：key=订单号_产品编号, value=当前标签编号
     * 用于管理每个订单每个产品当前正在采集的垛的标签编号
     * key格式：OrderNo_ProductNo
     */
    private final ConcurrentHashMap<String, String> currentTagNoMap = new ConcurrentHashMap<>();
    
    /**
     * 当前垛计数缓存：key=标签编号, value=已采集数量
     */
    private final ConcurrentHashMap<String, Integer> palletCountMap = new ConcurrentHashMap<>();
    
    /**
     * 无箱码模式待关联托盘码的TagNo缓存：key=订单号_产品编号, value=待关联的TagNo
     * 用于无箱码模式下，满垛后保存tagNo供托盘码关联使用
     * key格式：OrderNo_ProductNo
     */
    private final ConcurrentHashMap<String, String> pendingNoBoxTagNoMap = new ConcurrentHashMap<>();
    
    /**
     * 强制满垛标记缓存：key=订单号_产品编号, value=是否已点击强制满垛
     * 用于标记哪些订单的哪些产品已经点击了强制满垛，允许箱数不匹配
     * key格式：OrderNo_ProductNo
     */
    private final ConcurrentHashMap<String, Boolean> forcePalletFlagMap = new ConcurrentHashMap<>();
    
    /**
     * 生成缓存key
     * 用于currentTagNoMap、pendingNoBoxTagNoMap、forcePalletFlagMap
     * @param orderNo 订单号
     * @param productNo 产品编号
     * @return 缓存key，格式：OrderNo_ProductNo
     */
    private String generateCacheKey(String orderNo, String productNo) {
        if (orderNo == null || productNo == null) {
            throw new IllegalArgumentException("订单号和产品编号不能为空");
        }
        return orderNo + "_" + productNo;
    }
    
    /**
     * 系统启动时恢复未完成的垛的缓存
     * 优化：只恢复最近一个采集的生产订单的TagNo，使用分页只查询一条记录，提升性能并兼容SQL Server 2008
     */
    @javax.annotation.PostConstruct
    public void initCacheFromDatabase() {
        try {
            System.out.println("[TagNo恢复] 开始从数据库恢复未完成的垛...");
            
            // 优化：使用分页查询只查询最近一条未关联托盘码的记录（Page(1, 1)只查一条，兼容SQL Server 2008）
            Page<CodeRelationPO> page = new Page<>(1, 1);
            Page<CodeRelationPO> resultPage = codeRelationMapper.selectPage(
                page,
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getIsDel, 0)
                    .and(wrapper -> wrapper.isNull(CodeRelationPO::getBigSerialNumber)
                                          .or()
                                          .eq(CodeRelationPO::getBigSerialNumber, ""))
                    .orderByDesc(CodeRelationPO::getAddTime)
            );
            
            List<CodeRelationPO> records = resultPage.getRecords();
            if (records == null || records.isEmpty()) {
                System.out.println("[TagNo恢复] 没有未完成的垛，无需恢复");
                return;
            }
            
            // 只找最近的那个订单和产品（根据 AddTime 最大的记录）
            CodeRelationPO latestRecord = records.get(0); // 已按 AddTime 降序排序，第一条就是最新的
            String latestOrderNo = latestRecord.getOrderNo();
            String latestProductNo = latestRecord.getProductNo();  // 获取产品编号
            String latestTagNo = latestRecord.getTagNo();
            LocalDateTime latestAddTime = latestRecord.getAddTime();
            
            if (latestOrderNo == null || latestProductNo == null || latestTagNo == null) {
                System.out.println("[TagNo恢复] 最近记录缺少订单号、产品编号或TagNo，跳过恢复");
                return;
            }
            
            System.out.println("[TagNo恢复] 识别到最近的生产订单：订单=" + latestOrderNo + ", 产品=" + latestProductNo + ", TagNo=" + latestTagNo + ", 时间=" + latestAddTime);
            
            // 校验：检查该TagNo是否有记录的VirtualSerialNumber不为空
            // 如果有，说明该垛已完成托盘码关联，不应恢复
            Long virtualCodeCount = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getTagNo, latestTagNo)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .isNotNull(CodeRelationPO::getVirtualSerialNumber)
                    .ne(CodeRelationPO::getVirtualSerialNumber, "")
            );
            
            if (virtualCodeCount > 0) {
                System.out.println("[TagNo恢复] 跳过已完成垛：订单=" + latestOrderNo + ", 产品=" + latestProductNo + ", TagNo=" + latestTagNo 
                                 + ", 原因：该垛已有 " + virtualCodeCount + " 条记录完成了托盘码关联（VirtualSerialNumber不为空）");
                return;
            }
            
            // 统计该TagNo的记录数
            Long count = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getTagNo, latestTagNo)
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            // 获取该TagNo的Qty（每垛箱数）
            Integer qty = latestRecord.getQty();
            Integer type = latestRecord.getType();
            
            // 判断是否满垛
            boolean isFull = (qty != null && count >= qty);
            
            // 生成缓存key（格式：OrderNo_ProductNo）
            String cacheKey = generateCacheKey(latestOrderNo, latestProductNo);
            
            // 如果是无箱码模式（type=2）且已满垛，恢复到待关联Map
            // 否则恢复到当前采集Map
            if (type == 2 && isFull) {
                pendingNoBoxTagNoMap.put(cacheKey, latestTagNo);
                System.out.println("[TagNo恢复] 成功恢复无箱码待关联订单：订单=" + latestOrderNo + ", 产品=" + latestProductNo + ", TagNo=" + latestTagNo + ", 已采集=" + count + ", 每垛箱数=" + qty);
            } else {
                currentTagNoMap.put(cacheKey, latestTagNo);
            palletCountMap.put(latestTagNo, count.intValue());
                System.out.println("[TagNo恢复] 成功恢复正在采集的订单：订单=" + latestOrderNo + ", 产品=" + latestProductNo + ", TagNo=" + latestTagNo + ", 已采集=" + count + ", 每垛箱数=" + qty);
            }
            
            // 注意：布隆过滤器初始化已改为延迟加载（应用就绪后执行），不再在启动时同步初始化
            
        } catch (Exception e) {
            System.err.println("[TagNo恢复] 恢复失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 初始化全局箱码 Bloom Filter（异步执行）
     * 从数据库加载整个数据库所有未删除的箱码，添加到全局 Bloom Filter
     * 注意：只加载箱码（SmallSerialNumber），且只加载BigSerialNumber字段有值的数据
     * 
     * 优化说明：
     * 1. 使用 @Async 异步执行，不阻塞系统启动
     * 2. 使用专用线程池 bloomFilterInitExecutor
     * 3. 分批加载数据，每批200,000条，避免一次性加载大量数据导致内存溢出
     * 4. 每批之间休眠50ms，加快加载速度
     */
    @Async("bloomFilterInitExecutor")
    public void initGlobalBoxCodeBloomFilter() {
        try {
            long startTime = System.currentTimeMillis();
            System.out.println("[BloomFilter初始化] 开始异步加载全局箱码到 Bloom Filter");
            
            // 重建 Bloom Filter（清空旧数据）
            bloomFilterManager.rebuildFilter();
            
            // 先查询总记录数，用于验证是否全部加载
            Long totalRecords = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getIsDel, 0)
                    .isNotNull(CodeRelationPO::getBigSerialNumber)
                    .ne(CodeRelationPO::getBigSerialNumber, "")
            );
            System.out.println("[BloomFilter初始化] 数据库符合条件的总记录数: " + totalRecords);
            
            // 分批加载数据，每批 200,000 条
            int batchSize = 200000;
            int pageNum = 1;
            int totalCount = 0;
            int emptySmallSerialNumberCount = 0; // 统计SmallSerialNumber为空的记录数
            
            while (true) {
                // 检查中断标志，如果被中断则退出循环
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("[BloomFilter初始化] 检测到中断信号，停止初始化");
                    break;
                }
                
                // 分页查询数据（只加载BigSerialNumber字段有值的数据）
                Page<CodeRelationPO> page = new Page<>(pageNum, batchSize);
                Page<CodeRelationPO> resultPage = codeRelationMapper.selectPage(
                    page,
                    new LambdaQueryWrapper<CodeRelationPO>()
                        .eq(CodeRelationPO::getIsDel, 0)
                        .isNotNull(CodeRelationPO::getBigSerialNumber)
                        .ne(CodeRelationPO::getBigSerialNumber, "")
                        .select(CodeRelationPO::getSmallSerialNumber)
                );
                
                List<CodeRelationPO> records = resultPage.getRecords();
                if (records == null || records.isEmpty()) {
                    System.out.println("[BloomFilter初始化] 第 " + pageNum + " 批查询结果为空，退出循环");
                    break; // 没有更多数据，退出循环
                }
                
                // 批量添加箱码到全局 Bloom Filter
                List<String> boxCodeList = new ArrayList<>();
                for (CodeRelationPO code : records) {
                    if (StringUtils.hasText(code.getSmallSerialNumber())) {
                        boxCodeList.add(code.getSmallSerialNumber());
                    } else {
                        emptySmallSerialNumberCount++;
                    }
                }
                
                if (!boxCodeList.isEmpty()) {
                    bloomFilterManager.putAllBoxCodes(boxCodeList);
                    totalCount += boxCodeList.size();
                    System.out.println("[BloomFilter初始化] 已加载第 " + pageNum + " 批，本批查询 " + records.size() + " 条记录，有效码 " + boxCodeList.size() + " 个，累计 " + totalCount + " 个码");
                } else if (records.size() > 0) {
                    System.out.println("[BloomFilter初始化] 第 " + pageNum + " 批查询到 " + records.size() + " 条记录，但所有记录的SmallSerialNumber都为空");
                }
                
                // 检查是否还有更多数据
                // 如果当前批次返回的数据量小于批次大小，说明已经是最后一页
                if (records.size() < batchSize) {
                    System.out.println("[BloomFilter初始化] 当前批次数据量(" + records.size() + ")小于批次大小(" + batchSize + ")，已到达最后一页");
                    break;
                }
                
                // 如果已经是最后一页，退出循环（双重检查，确保不会遗漏数据）
                if (pageNum >= resultPage.getPages()) {
                    System.out.println("[BloomFilter初始化] 已到达最后一页（第 " + pageNum + " 页，共 " + resultPage.getPages() + " 页）");
                    break;
                }
                
                pageNum++;
                
                // 每批之间休眠 50ms，加快加载速度
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // 捕获中断异常，设置中断标志并退出循环
                    Thread.currentThread().interrupt();
                    System.out.println("[BloomFilter初始化] 初始化被中断，停止加载");
                    break;
                }
                
                // 再次检查中断标志（双重检查）
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("[BloomFilter初始化] 检测到中断信号，停止初始化");
                    break;
                }
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            if (totalCount > 0) {
                System.out.println("[BloomFilter初始化] 异步初始化完成！");
                System.out.println("  - 数据库符合条件的总记录数: " + totalRecords);
                System.out.println("  - 成功加载的码数量: " + totalCount);
                System.out.println("  - SmallSerialNumber为空的记录数: " + emptySmallSerialNumberCount);
                System.out.println("  - 耗时: " + duration + " ms (" + (duration / 1000.0) + " 秒)");
                if (totalRecords != null && totalCount + emptySmallSerialNumberCount < totalRecords) {
                    System.out.println("  - ⚠️ 警告：可能存在数据未完全加载（总记录数: " + totalRecords + ", 已处理: " + (totalCount + emptySmallSerialNumberCount) + "）");
                }
            } else {
                System.out.println("[BloomFilter初始化] 数据库中没有箱码数据");
            }
            
        } catch (Exception e) {
            System.err.println("[BloomFilter初始化] 异步初始化失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 添加码（手动添加）
     * 前端点击"添加码"按钮后调用，直接生成系统箱码并插入数据库
     * 按照无码规则生成一条记录
     * 
     * 注意：request.getBoxCount() 来自前端的采集规格字段（collectionSpecField）
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized ApiResult<GenerateSystemCodeResult> addCode(CodeAddRequest request) {
        try {
            if (!StringUtils.hasText(request.getOrderNo())) {
                return ApiResult.error("订单编号不能为空");
            }
            if (request.getBoxCount() == null || request.getBoxCount() <= 0) {
                return ApiResult.error("每垛箱数必须大于0，请检查前端的采集规格设置");
            }
            
            String orderNo = request.getOrderNo();
            String productNo = request.getProductNo();
            Integer boxesPerPallet = request.getBoxCount(); // 从前端采集规格获取
            
            System.out.println("[手动添加码] 订单=" + orderNo + ", 产品=" + productNo + ", 每垛箱数=" + boxesPerPallet);
            
            // 生成系统箱码（22位pzmz码）
            String systemBoxCode = generateUniqueBoxCode(orderNo);
            System.out.println("[手动添加码] 系统生成箱码：" + systemBoxCode);
            
            // 获取或生成当前订单产品的TagNo
            String cacheKey = generateCacheKey(orderNo, productNo);
            String tagNo = currentTagNoMap.computeIfAbsent(cacheKey, k -> generateNewTagNo());
            
            // 获取当前垛已采集数量
            Integer currentCount = palletCountMap.getOrDefault(tagNo, 0);
            
            // 构建实体并插入
            CodeRelationPO entity = new CodeRelationPO();
            entity.setSmallSerialNumber(systemBoxCode); // 系统生成的箱码
            entity.setProductNo(request.getProductNo());
            entity.setOrderNo(orderNo);
            entity.setBatchNo(request.getBatchNo());
            entity.setTagNo(tagNo);
            entity.setType(request.getType());
            entity.setQty(boxesPerPallet); // 使用从前端采集规格获取的每垛箱数
            entity.setStatus(0);
            entity.setIsVirtual(0); // 初始设为0，虚拟垛标生成后才会更新为1
            entity.setIsUpload(1);
            entity.setIsDel(0);
            entity.setAddTime(LocalDateTime.now());
            entity.setErrCount(0);
            entity.setBigSerialNumber("");
            entity.setBiggerSerialNumber("");
            entity.setMediumSerialNumber("");
            entity.setVirtualSerialNumber("");
            entity.setDxCode("");
            entity.setSalesCode("");
            
            codeRelationMapper.insert(entity);
            
            // 更新计数
            currentCount++;
            palletCountMap.put(tagNo, currentCount);
            
            // 判断是否满垛
            boolean isPalletFull = currentCount >= boxesPerPallet;
            
            if (isPalletFull) {
                // 满垛后清除缓存，下一次采集将生成新的TagNo
                currentTagNoMap.remove(cacheKey);
                palletCountMap.remove(tagNo);
                
                System.out.println("[手动添加码] 满垛！订单=" + orderNo + ", 产品=" + productNo + ", TagNo=" + tagNo + ", 数量=" + currentCount);
            }
            
            System.out.println("[手动添加码] 成功：订单=" + orderNo + ", 系统箱码=" + systemBoxCode 
                             + ", TagNo=" + tagNo + ", 当前数量=" + currentCount + "/" + boxesPerPallet
                             + ", 满垛=" + isPalletFull);
            
            return ApiResult.success("系统生成箱码成功", 
                GenerateSystemCodeResult.success(tagNo, systemBoxCode, currentCount, "系统生成箱码成功"));
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("添加码失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除码（清除上一个采集的箱码）
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized ApiResult<Boolean> deleteCode(String smallSerialNumber, String orderNo, String productNo) {
        try {
            if (!StringUtils.hasText(orderNo)) {
                return ApiResult.error("订单编号不能为空");
            }
            if (!StringUtils.hasText(productNo)) {
                return ApiResult.error("产品编号不能为空");
            }
            
            String cacheKey = generateCacheKey(orderNo, productNo);
            String tagNo = currentTagNoMap.get(cacheKey);
            if (tagNo == null || tagNo.isEmpty()) {
                return ApiResult.error("当前订单产品没有正在采集的垛，无法删除箱码");
            }
            
            Integer currentCount = palletCountMap.getOrDefault(tagNo, 0);
            if (currentCount == 0) {
                return ApiResult.error("当前垛没有采集任何箱码，无法删除");
            }
            
            List<CodeRelationPO> lastRecords = codeRelationMapper.selectList(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .orderByDesc(CodeRelationPO::getAddTime)
            );
            
            if (lastRecords.isEmpty()) {
                return ApiResult.error("未找到最后一条采集记录");
            }
            
            CodeRelationPO lastRecord = lastRecords.get(0);
            
            LambdaUpdateWrapper<CodeRelationPO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(CodeRelationPO::getId, lastRecord.getId())
                         .set(CodeRelationPO::getIsDel, 1);
            
            codeRelationMapper.update(null, updateWrapper);
            palletCountMap.put(tagNo, currentCount - 1);
            
            // 从布隆过滤器中删除该箱码
            if (StringUtils.hasText(lastRecord.getSmallSerialNumber())) {
                bloomFilterManager.removeBoxCode(lastRecord.getSmallSerialNumber());
            }
            
            return ApiResult.success("删除上一个箱码成功", true);
        } catch (Exception e) {
            return ApiResult.error("删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据箱码删除（数据上传页面使用）
     * 删除CodeRelationUpload表中的数据，并更新相关Qty和OrderCount、OrderSumCount
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized ApiResult<Boolean> deleteByBoxCode(String boxCode) {
        try {
            if (!StringUtils.hasText(boxCode)) {
                // 记录操作日志（箱码为空）
                try {
                    OperateLog operateLog = new OperateLog();
                    operateLog.setModuleName(ModuleNameEnum.CODE_QUERY.getDescription());
                    operateLog.setOperateType(OperateTypeEnum.DELETE.getDescription());
                    operateLog.setTargetId("");
                    operateLog.setTargetName("");
                    operateLog.setOperateContent("码删除失败: 箱码不能为空");
                    operateLog.setOperateResult("失败");
                    operateLog.setFailReason("箱码不能为空");
                    operateLog.setOperateTime(LocalDateTime.now());
                    operateLog.setOperatorName("系统");
                    operateLog.setType(1);
                    operateLog.setDeviceInfo("数据上传页面");
                    operateLogApplicationService.saveLogAsync(operateLog);
                } catch (Exception logException) {
                    System.err.println("[根据箱码删除] 记录操作日志失败: " + logException.getMessage());
                }
                return ApiResult.error("箱码不能为空");
            }
            
            System.out.println("[根据箱码删除] 开始删除箱码: " + boxCode);
            
            // 1. 查询CodeRelationUpload表中该箱码的记录
            List<CodeRelationPO> records = codeRelationUploadMapper.selectList(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getSmallSerialNumber, boxCode)
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            if (records.isEmpty()) {
                // 记录操作日志（未找到记录）
                try {
                    OperateLog operateLog = new OperateLog();
                    operateLog.setModuleName(ModuleNameEnum.CODE_QUERY.getDescription());
                    operateLog.setOperateType(OperateTypeEnum.DELETE.getDescription());
                    operateLog.setTargetId(boxCode);
                    operateLog.setTargetName("");
                    operateLog.setOperateContent("码删除失败: 未找到该箱码的记录 - " + boxCode);
                    operateLog.setOperateResult("失败");
                    operateLog.setFailReason("未找到该箱码的记录");
                    operateLog.setOperateTime(LocalDateTime.now());
                    operateLog.setOperatorName("系统");
                    operateLog.setType(1);
                    operateLog.setDeviceInfo("数据上传页面");
                    operateLogApplicationService.saveLogAsync(operateLog);
                } catch (Exception logException) {
                    System.err.println("[根据箱码删除] 记录操作日志失败: " + logException.getMessage());
                }
                return ApiResult.error("未找到该箱码的记录");
            }
            
            if (records.size() > 1) {
                // 记录操作日志（数据异常）
                try {
                    OperateLog operateLog = new OperateLog();
                    operateLog.setModuleName(ModuleNameEnum.CODE_QUERY.getDescription());
                    operateLog.setOperateType(OperateTypeEnum.DELETE.getDescription());
                    operateLog.setTargetId(boxCode);
                    operateLog.setTargetName("");
                    operateLog.setOperateContent("码删除失败: 发现多条记录，数据异常 - " + boxCode);
                    operateLog.setOperateResult("失败");
                    operateLog.setFailReason("发现多条记录，数据异常");
                    operateLog.setOperateTime(LocalDateTime.now());
                    operateLog.setOperatorName("系统");
                    operateLog.setType(1);
                    operateLog.setDeviceInfo("数据上传页面");
                    operateLogApplicationService.saveLogAsync(operateLog);
                } catch (Exception logException) {
                    System.err.println("[根据箱码删除] 记录操作日志失败: " + logException.getMessage());
                }
                return ApiResult.error("发现多条记录，数据异常");
            }
            
            CodeRelationPO record = records.get(0);
            String orderNo = record.getOrderNo();
            String productNo = record.getProductNo();
            String tagNo = record.getTagNo();
            
            System.out.println("[根据箱码删除] 找到记录: 订单=" + orderNo + ", 产品=" + productNo + ", TagNo=" + tagNo);
            
            // 保存删除前的数据（用于操作日志）
            String beforeDataJson = null;
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                beforeDataJson = objectMapper.writeValueAsString(record);
            } catch (Exception e) {
                System.err.println("[根据箱码删除] 序列化删除前数据失败: " + e.getMessage());
            }
            
            // 2. 逻辑删除该记录
            int deleteCount = codeRelationUploadMapper.update(null,
                new LambdaUpdateWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getId, record.getId())
                    .set(CodeRelationPO::getIsDel, 1)
            );
            
            if (deleteCount == 0) {
                // 记录操作日志（删除失败）
                try {
                    OperateLog operateLog = new OperateLog();
                    operateLog.setModuleName(ModuleNameEnum.CODE_QUERY.getDescription());
                    operateLog.setOperateType(OperateTypeEnum.DELETE.getDescription());
                    operateLog.setTargetId(boxCode);
                    operateLog.setTargetName(orderNo);
                    operateLog.setOperateContent("码删除失败: 删除记录失败 - 箱码=" + boxCode + ", 订单=" + orderNo);
                    operateLog.setOperateResult("失败");
                    operateLog.setFailReason("删除记录失败");
                    operateLog.setBeforeData(beforeDataJson);
                    operateLog.setOperateTime(LocalDateTime.now());
                    operateLog.setOperatorName("系统");
                    operateLog.setType(1);
                    operateLog.setDeviceInfo("数据上传页面");
                    operateLogApplicationService.saveLogAsync(operateLog);
                } catch (Exception logException) {
                    System.err.println("[根据箱码删除] 记录操作日志失败: " + logException.getMessage());
                }
                return ApiResult.error("删除记录失败");
            }
            
            System.out.println("[根据箱码删除] 已逻辑删除记录ID=" + record.getId());
            
            // 3. 查询该TagNo下剩余的未删除记录数量
            Long remainingCount = codeRelationUploadMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getProductNo, productNo)
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            System.out.println("[根据箱码删除] TagNo=" + tagNo + " 剩余记录数=" + remainingCount);
            
            // 4. 更新该TagNo下所有记录的Qty字段（减1）
            int updateQtyCount = codeRelationUploadMapper.update(null,
                new LambdaUpdateWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getProductNo, productNo)
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .set(CodeRelationPO::getQty, remainingCount.intValue())
            );
            
            System.out.println("[根据箱码删除] 更新Qty字段: 影响记录数=" + updateQtyCount + ", 新Qty=" + remainingCount);
            
            // 5. 更新ProductionOrderDetail表中的OrderCount（减1）
            ProductionOrderDetailPO orderDetail = productionOrderDetailMapper.selectOne(
                new LambdaQueryWrapper<ProductionOrderDetailPO>()
                    .eq(ProductionOrderDetailPO::getOrderNo, orderNo)
                    .eq(ProductionOrderDetailPO::getProductNo, productNo)
                    .eq(ProductionOrderDetailPO::getIsDel, 0)
            );
            
            if (orderDetail != null) {
                Integer currentOrderCount = orderDetail.getOrderCount();
                if (currentOrderCount != null && currentOrderCount > 0) {
                    int newOrderCount = currentOrderCount - 1;
                    productionOrderDetailMapper.update(null,
                        new LambdaUpdateWrapper<ProductionOrderDetailPO>()
                            .eq(ProductionOrderDetailPO::getId, orderDetail.getId())
                            .set(ProductionOrderDetailPO::getOrderCount, newOrderCount)
                    );
                    System.out.println("[根据箱码删除] 更新OrderCount: " + currentOrderCount + " -> " + newOrderCount);
                }
            }
            
            // 6. 更新ProductionOrder表中的OrderSumCount（减1）
            ProductionOrderPO productionOrder = productionOrderMapper.selectOne(
                new LambdaQueryWrapper<ProductionOrderPO>()
                    .eq(ProductionOrderPO::getOrderNo, orderNo)
            );
            
            if (productionOrder != null) {
                Integer currentSumCount = productionOrder.getOrderSumCount();
                if (currentSumCount != null && currentSumCount > 0) {
                    int newSumCount = currentSumCount - 1;
                    productionOrderMapper.update(null,
                        new LambdaUpdateWrapper<ProductionOrderPO>()
                            .eq(ProductionOrderPO::getId, productionOrder.getId())
                            .set(ProductionOrderPO::getOrderSumCount, newSumCount)
                    );
                    System.out.println("[根据箱码删除] 更新OrderSumCount: " + currentSumCount + " -> " + newSumCount);
                }
            }
            
            System.out.println("[根据箱码删除] 删除成功: 箱码=" + boxCode);
            
            // 从布隆过滤器中删除该箱码
            if (StringUtils.hasText(boxCode)) {
                bloomFilterManager.removeBoxCode(boxCode);
            }
            
            // 记录操作日志（成功）
            try {
                OperateLog operateLog = new OperateLog();
                operateLog.setModuleName(ModuleNameEnum.CODE_QUERY.getDescription()); // 使用码查询模块
                operateLog.setOperateType(OperateTypeEnum.DELETE.getDescription()); // 删除操作
                operateLog.setTargetId(boxCode); // 目标ID：箱码
                operateLog.setTargetName(orderNo); // 目标名称：订单号
                operateLog.setOperateContent("码删除: 箱码=" + boxCode + ", 订单=" + orderNo + ", 产品=" + productNo + ", TagNo=" + tagNo + ", 剩余箱数=" + remainingCount);
                operateLog.setBeforeData(beforeDataJson); // 删除前的数据
                operateLog.setOperateResult("成功");
                operateLog.setOperateTime(LocalDateTime.now());
                operateLog.setOperatorName("系统");
                operateLog.setType(1); // 操作日志
                operateLog.setDeviceInfo("数据上传页面");
                
                // 异步保存日志，不阻塞主流程
                operateLogApplicationService.saveLogAsync(operateLog);
                System.out.println("[根据箱码删除] 操作日志已记录");
            } catch (Exception logException) {
                // 日志记录失败不影响业务流程
                System.err.println("[根据箱码删除] 记录操作日志失败: " + logException.getMessage());
            }
            
            return ApiResult.success("删除码成功", true);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[根据箱码删除] 删除失败: " + e.getMessage());
            
            // 记录操作日志（失败）
            try {
                OperateLog operateLog = new OperateLog();
                operateLog.setModuleName(ModuleNameEnum.CODE_QUERY.getDescription()); // 使用码查询模块
                operateLog.setOperateType(OperateTypeEnum.DELETE.getDescription()); // 删除操作
                operateLog.setTargetId(boxCode); // 目标ID：箱码
                operateLog.setTargetName(""); // 目标名称：删除失败时可能没有订单号
                operateLog.setOperateContent("码删除失败: 箱码=" + boxCode);
                operateLog.setOperateResult("失败");
                operateLog.setFailReason(e.getMessage());
                operateLog.setOperateTime(LocalDateTime.now());
                operateLog.setOperatorName("系统");
                operateLog.setType(1); // 操作日志
                operateLog.setDeviceInfo("数据上传页面");
                
                // 异步保存日志，不阻塞主流程
                operateLogApplicationService.saveLogAsync(operateLog);
                System.out.println("[根据箱码删除] 失败操作日志已记录");
            } catch (Exception logException) {
                // 日志记录失败不影响业务流程
                System.err.println("[根据箱码删除] 记录失败操作日志失败: " + logException.getMessage());
            }
            
            return ApiResult.error("删除码失败：" + e.getMessage());
        }
    }
    
    private String generateTagNo(String orderNo) {
        return orderNo + "_TAG";
    }
    
    private void batchUpdateVirtualSerialNumber(String tagNo, Integer stackCount) {
        List<CodeRelationPO> codeList = codeRelationMapper.selectList(
            new LambdaQueryWrapper<CodeRelationPO>()
                .eq(CodeRelationPO::getTagNo, tagNo)
                .eq(CodeRelationPO::getIsDel, 0)
                .and(wrapper -> wrapper.isNull(CodeRelationPO::getVirtualSerialNumber)
                                      .or()
                                      .eq(CodeRelationPO::getVirtualSerialNumber, ""))
        );
        
        if (codeList.isEmpty()) {
            return;
        }
        
        String actualPalletCode = stackCount != null ? stackCount.toString() : "001";
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomNum = String.format("%03d", new Random().nextInt(1000));
        String virtualSerialNumber = "#" + actualPalletCode + "#_" + dateStr + randomNum + "C";
        
        LambdaUpdateWrapper<CodeRelationPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CodeRelationPO::getTagNo, tagNo)
                     .eq(CodeRelationPO::getIsDel, 0)
                     .and(wrapper -> wrapper.isNull(CodeRelationPO::getVirtualSerialNumber)
                                           .or()
                                           .eq(CodeRelationPO::getVirtualSerialNumber, ""));
        
        CodeRelationPO updateEntity = new CodeRelationPO();
        updateEntity.setVirtualSerialNumber(virtualSerialNumber);
        updateEntity.setUploadTime(LocalDateTime.now());
        
        codeRelationMapper.update(updateEntity, updateWrapper);
    }
    
    /**
     * 替换码
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Boolean> replaceCode(CodeReplaceRequest request) {
        try {
            if (!StringUtils.hasText(request.getOldCode())) {
                return ApiResult.error("原码不能为空");
            }
            if (!StringUtils.hasText(request.getNewCode())) {
                return ApiResult.error("新码不能为空");
            }
            if (request.getOldCode().equals(request.getNewCode())) {
                return ApiResult.error("原码和新码不能相同");
            }
            
            CodeRelationPO oldCodeRecord = codeRelationMapper.selectOne(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getSmallSerialNumber, request.getOldCode())
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            if (oldCodeRecord == null) {
                return ApiResult.error("原码不存在或已被删除");
            }
            
            CodeRelationPO existingNewCode = codeRelationMapper.selectOne(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getSmallSerialNumber, request.getNewCode())
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            if (existingNewCode != null) {
                return ApiResult.error("新码已存在，无法替换");
            }
            
            CodeRelationPO updateEntity = new CodeRelationPO();
            updateEntity.setSmallSerialNumber(request.getNewCode());
            if (StringUtils.hasText(request.getReason())) {
                updateEntity.setMsg(request.getReason());
            }
            updateEntity.setUploadTime(LocalDateTime.now());
            
            codeRelationMapper.update(updateEntity, 
                new LambdaUpdateWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getSmallSerialNumber, request.getOldCode())
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            return ApiResult.success("码替换成功", true);
        } catch (Exception e) {
            return ApiResult.error("码替换失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询码信息
     * 支持查询箱码（SmallSerialNumber）或托盘码（BigSerialNumber）
     * 返回同一VirtualSerialNumber的所有未删除记录
     */
    public ApiResult<List<CodeQueryVO>> queryCode(String code) {
        try {
            if (!StringUtils.hasText(code)) {
                return ApiResult.error("查询码不能为空");
            }
            
            // 1. 先尝试按箱码查询，如果查不到则按托盘码查询
            List<CodeRelationPO> codeRecords = codeRelationMapper.selectList(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getIsDel, 0)
                    .and(wrapper -> wrapper
                        .eq(CodeRelationPO::getSmallSerialNumber, code)
                        .or()
                        .eq(CodeRelationPO::getBigSerialNumber, code))
            );
            
            if (codeRecords == null || codeRecords.isEmpty()) {
                return ApiResult.error("未找到该码信息");
            }
            
            // 2. 获取第一条记录的VirtualSerialNumber
            String virtualSerialNumber = codeRecords.get(0).getVirtualSerialNumber();
            
            // 3. 如果VirtualSerialNumber为空或空字符串，只返回匹配到的那一条数据
            List<CodeRelationPO> finalRecords;
            if (!StringUtils.hasText(virtualSerialNumber)) {
                // 只返回第一条匹配的记录（该条数据）
                finalRecords = Collections.singletonList(codeRecords.get(0));
            } else {
                // 4. 根据VirtualSerialNumber查询所有相同VirtualSerialNumber的记录
                finalRecords = codeRelationMapper.selectList(
                    new LambdaQueryWrapper<CodeRelationPO>()
                        .eq(CodeRelationPO::getVirtualSerialNumber, virtualSerialNumber)
                        .eq(CodeRelationPO::getIsDel, 0)
                        .orderByAsc(CodeRelationPO::getAddTime)
                );
            }
            
            if (finalRecords.isEmpty()) {
                return ApiResult.error("未找到该码的关联信息");
            }
            
            // 5. 查询生产订单详情（只查一次，所有记录共用）
            // 一个订单可能有多个产品明细，使用ProductNO来匹配
            ProductionOrderDetailPO orderDetail = null;
            String orderNo = finalRecords.get(0).getOrderNo();
            String productNo = finalRecords.get(0).getProductNo();
            if (StringUtils.hasText(orderNo) && StringUtils.hasText(productNo)) {
                Page<ProductionOrderDetailPO> page = new Page<>(1, 1);
                Page<ProductionOrderDetailPO> orderPage = productionOrderDetailMapper.selectPage(page,
                    new LambdaQueryWrapper<ProductionOrderDetailPO>()
                        .eq(ProductionOrderDetailPO::getOrderNo, orderNo)
                        .eq(ProductionOrderDetailPO::getProductNo, productNo)
                        .eq(ProductionOrderDetailPO::getIsDel, 0)
                );
                List<ProductionOrderDetailPO> orderList = orderPage.getRecords();
                if (orderList != null && !orderList.isEmpty()) {
                    orderDetail = orderList.get(0);
                }
            }
            
            // 6. 组装返回结果列表
            List<CodeQueryVO> resultList = new ArrayList<>();
            for (CodeRelationPO record : finalRecords) {
            CodeQueryVO vo = new CodeQueryVO();
                vo.setSmallSerialNumber(record.getSmallSerialNumber());
                vo.setBigSerialNumber(record.getBigSerialNumber());
                vo.setBiggerSerialNumber(record.getBiggerSerialNumber());
                vo.setMediumSerialNumber(record.getMediumSerialNumber());
                vo.setVirtualSerialNumber(record.getVirtualSerialNumber());
                vo.setIsVirtual(record.getIsVirtual());
                vo.setDxCode(record.getDxCode());
                vo.setSalesCode(record.getSalesCode());
                vo.setTagNo(record.getTagNo());
                vo.setAddTime(record.getAddTime());
                vo.setUploadTime(record.getUploadTime());
                vo.setProductNo(record.getProductNo());
                vo.setOrderNo(record.getOrderNo());
                vo.setBatchNo(record.getBatchNo());
                vo.setType(record.getType());
                vo.setStatus(record.getStatus());
                
                // 比率字段：使用 CodeRelationPO 的 Qty 字段（每垛箱数）
                vo.setRatio(record.getQty());
            
            if (orderDetail != null) {
                vo.setProductName(orderDetail.getProductName());
                vo.setProductFormatName(orderDetail.getProductFormatName());
                vo.setSyBatchNo(orderDetail.getSyBatchNo());
                vo.setProductTime(orderDetail.getProductTime());
                vo.setTwillendTime(orderDetail.getTwillendTime());
            }
            
                resultList.add(vo);
            }
            
            return ApiResult.success("查询成功", resultList);
        } catch (Exception e) {
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 码校验（设备类别：码校验）
     * 不再校验长度，所有码均返回合格
     */
    public ApiResult<String> validateCode(String code) {
        try {
            // 空码或null返回不合格
            if (code == null || code.isEmpty()) {
                return ApiResult.success("校验完成", "02");
            }
            
            // 所有非空码均返回合格（01）
            return ApiResult.success("校验完成", "01");
        } catch (Exception e) {
            return ApiResult.error("校验失败：" + e.getMessage());
        }
    }
    
    /**
     * 读码剔除校验
     */
    public ApiResult<CodeRejectResult> validateCodeReject(CodeRejectRequest request) {
        try {
            String code = request.getCode();
            String orderNo = request.getOrderNo();
            
            System.out.println("[读码剔除校验] 开始校验 - 码=" + code + ", 订单=" + orderNo);
            
            // 1. 无码剔除
            if (code.equals("null") || code.isEmpty()) {
                System.out.println("[读码剔除校验] 无码 - 剔除");
                return ApiResult.success("读码剔除校验完成", CodeRejectResult.reject("NO_CODE", "无码，剔除"));
            }
            
            // 2. 重码校验（使用全局 Bloom Filter 快速过滤）
            // 注意：只校验箱码（SmallSerialNumber），校验整个数据库未删除的箱码
            
            // 先用 Bloom Filter 快速判断箱码是否可能存在
            boolean mightExist = bloomFilterManager.mightContainBoxCode(code);
            
            if (mightExist) {
                // Bloom Filter 显示可能存在，再查数据库确认（只查箱码）
                System.out.println("[读码剔除校验-BloomFilter] 码可能重复，查询数据库确认：" + code);
                
                Long smallCodeCount = codeRelationMapper.selectCount(
                    new LambdaQueryWrapper<CodeRelationPO>()
                        .eq(CodeRelationPO::getSmallSerialNumber, code)
                        .eq(CodeRelationPO::getIsDel, 0)
                );
                
                if (smallCodeCount > 0) {
                    System.out.println("[读码剔除校验] 重码 - 剔除 (SmallCode=" + smallCodeCount + ", BigCode=" + 0 + ")");
                    return ApiResult.success("读码剔除校验完成", CodeRejectResult.reject("DUPLICATE", "重复码，剔除"));
                } else {
                    System.out.println("[读码剔除校验-BloomFilter] Bloom Filter误判，码不重复");
                }
            } else {
                // Bloom Filter 显示一定不存在，直接跳过数据库查询
                System.out.println("[读码剔除校验-BloomFilter] 码不存在，跳过数据库查询");
            }
            
            // 3. 合格，放行
            System.out.println("[读码剔除校验] 合格 - 放行");
            return ApiResult.success("读码剔除校验完成", CodeRejectResult.pass());
        } catch (Exception e) {
            System.err.println("[读码剔除校验] 异常: " + e.getMessage());
            e.printStackTrace();
            return ApiResult.error("读码剔除校验失败：" + e.getMessage());
        }
    }
    
    /**
     * 箱码采集（设备类别：箱码采集）
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized ApiResult<CodeCollectResult> collectBoxCode(CodeCollectRequest request) {
        try {
            if (!StringUtils.hasText(request.getOrderNo())) {
                return ApiResult.error("订单编号不能为空");
            }
            if (request.getBoxesPerPallet() == null || request.getBoxesPerPallet() <= 0) {
                return ApiResult.error("每垛箱数必须大于0");
            }
            
            String boxCode = request.getBoxCode();
            String orderNo = request.getOrderNo();
            String productNo = request.getProductNo();
            Integer boxesPerPallet = request.getBoxesPerPallet();
            
            // 判断是否无码（包括null、空字符串、"null"字符串）
            boolean isNoCode = !StringUtils.hasText(boxCode) || "null".equalsIgnoreCase(boxCode);
            
            // 如果是无码，生成系统箱码
            String actualBoxCode = boxCode;
            if (isNoCode) {
                actualBoxCode = generateUniqueBoxCode(orderNo);
                System.out.println("[箱码采集] 无码情况，系统生成箱码：" + actualBoxCode);
            } else {
                // 有码情况：使用 Bloom Filter 优化重码检查（只校验箱码）
                // 1. 先用 Bloom Filter 快速判断箱码是否可能存在
                boolean mightExist = bloomFilterManager.mightContainBoxCode(boxCode);
                
                if (mightExist) {
                    // 2. Bloom Filter 显示可能存在，再查数据库确认
                    System.out.println("[箱码采集-BloomFilter] 码可能重复，查询数据库确认：" + boxCode);
                    
                    // 只查询箱码（SmallSerialNumber），查询整个数据库未删除的记录
                    Long smallCodeCount = codeRelationMapper.selectCount(
                        new LambdaQueryWrapper<CodeRelationPO>()
                            .eq(CodeRelationPO::getSmallSerialNumber, boxCode)
                            .eq(CodeRelationPO::getIsDel, 0)
                    );
                    
                    if (smallCodeCount > 0) {
                        System.out.println("[箱码采集] 重复码：" + boxCode + " (SmallCode=" + smallCodeCount + ", BigCode=" + 0 + ")");
                        return ApiResult.success("采集处理完成", CodeCollectResult.duplicate(boxCode));
                    } else {
                        System.out.println("[箱码采集-BloomFilter] Bloom Filter误判，码不重复");
                    }
                } else {
                    // 3. Bloom Filter 显示一定不存在，直接跳过数据库查询
                    System.out.println("[箱码采集-BloomFilter] 码不存在，跳过数据库查询");
                }
            }
            
            // 获取或生成当前订单产品的标签编号
            String cacheKey = generateCacheKey(orderNo, productNo);
            String tagNo = currentTagNoMap.computeIfAbsent(cacheKey, k -> generateNewTagNo());
            
            // 获取当前垛已采集数量
            Integer currentCount = palletCountMap.getOrDefault(tagNo, 0);
            
            // 构建实体并存入数据库
            CodeRelationPO entity = new CodeRelationPO();
            entity.setSmallSerialNumber(actualBoxCode);
            entity.setVirtualSerialNumber("");
            entity.setProductNo(request.getProductNo());
            entity.setOrderNo(orderNo);
            entity.setBatchNo(request.getBatchNo());
            entity.setTagNo(tagNo);
            entity.setType(request.getType());
            entity.setQty(boxesPerPallet);
            entity.setIsVirtual(0);
            entity.setStatus(0);
            entity.setIsUpload(1);
            entity.setIsDel(0);
            entity.setAddTime(LocalDateTime.now());
            entity.setErrCount(0);
            entity.setWarehouseNo("001");
            entity.setTeamName(request.getTeamName());
            entity.setBigSerialNumber("");
            entity.setBiggerSerialNumber("");
            entity.setMediumSerialNumber("");
            entity.setDxCode("");
            entity.setSalesCode("");
            entity.setMsg("");
            
            codeRelationMapper.insert(entity);
            
            // 将箱码添加到全局 Bloom Filter
            bloomFilterManager.putBoxCode(actualBoxCode);
            
            // 更新计数
            currentCount++;
            palletCountMap.put(tagNo, currentCount);
            
            // 判断是否满垛
            boolean isPalletFull = currentCount >= boxesPerPallet;
            
            if (isPalletFull) {
                // 无论有箱码还是无箱码模式，满垛后都要清除当前垛的缓存
                currentTagNoMap.remove(cacheKey);
                palletCountMap.remove(tagNo);
                
                // 如果是无箱码模式（type=2），将tagNo保存到待关联Map中
                if (request.getType() == 2) {
                    pendingNoBoxTagNoMap.put(cacheKey, tagNo);
                    System.out.println("[箱码采集-无箱码] 满垛，订单=" + orderNo + ", 产品=" + productNo + ", TagNo=" + tagNo + " 已保存到待关联Map，等待托盘码关联");
                }
            }
            
            // 返回结果
            if (isNoCode) {
                return ApiResult.success("采集处理完成", 
                    CodeCollectResult.noCode(tagNo, currentCount, boxesPerPallet, isPalletFull, actualBoxCode));
            } else {
                return ApiResult.success("采集处理完成", 
                    CodeCollectResult.success(tagNo, currentCount, boxesPerPallet, isPalletFull));
            }
        } catch (Exception e) {
            return ApiResult.error("采集失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成唯一的箱码（用于无码情况）
     */
    private String generateUniqueBoxCode(String orderNo) {
        for (int attempt = 0; attempt < 100; attempt++) {
            long timestamp = System.currentTimeMillis();
            String timestampPart = String.valueOf(timestamp).substring(3);
            
            Random random = new Random();
            StringBuilder randomPart = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                randomPart.append(random.nextInt(10));
            }
            
            String generatedCode = "pzmz" + timestampPart + randomPart.toString();
            
            Long count = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getSmallSerialNumber, generatedCode)
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            if (count == 0) {
                return generatedCode;
            }
            
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        throw new RuntimeException("生成唯一箱码失败，超过最大尝试次数");
    }
    
    /**
     * 生成新的标签编号
     */
    private String generateNewTagNo() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
    
    /**
     * 托盘码关联（设备类别：托盘码关联、箱码关联）
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<PalletAssociateResult> associatePalletCode(PalletAssociateRequest request) {
        try {
            if (!StringUtils.hasText(request.getPalletCode())) {
                return ApiResult.error("托盘码不能为空");
            }
            if (!StringUtils.hasText(request.getOrderNo())) {
                return ApiResult.error("订单编号不能为空");
            }
            if (!StringUtils.hasText(request.getTriggerBoxCode())) {
                return ApiResult.error("触发箱码不能为空");
            }
            if (request.getQty() == null || request.getQty() <= 0) {
                return ApiResult.error("每垛箱数（Qty）必须大于0");
            }
            
            String palletCode = request.getPalletCode();
            String orderNo = request.getOrderNo();
            String triggerBoxCode = request.getTriggerBoxCode();
            Integer qty = request.getQty();
            
            System.out.println("[托盘码关联] 订单=" + orderNo + ", 触发箱码=" + triggerBoxCode + ", 托盘码=" + palletCode + ", Qty=" + qty);
            
            // 1. 根据触发箱码查找TagNo
            List<CodeRelationPO> triggerRecords = codeRelationMapper.selectList(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getSmallSerialNumber, triggerBoxCode)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .orderByDesc(CodeRelationPO::getAddTime)
            );
            
            if (triggerRecords.isEmpty()) {
                return ApiResult.error("未找到触发箱码对应的记录: " + triggerBoxCode);
            }
            
            CodeRelationPO triggerRecord = triggerRecords.get(0);
            String tagNo = triggerRecord.getTagNo();
            
            System.out.println("[托盘码关联] 找到触发箱码的TagNo: " + tagNo);
            
            // 2. 获取当前订单产品正在采集的TagNo（用于日志对比）
            // 从triggerRecord获取productNo
            String productNo = triggerRecord.getProductNo();
            String cacheKey = generateCacheKey(orderNo, productNo);
            String currentTagNo = currentTagNoMap.get(cacheKey);
            if (currentTagNo != null && !tagNo.equals(currentTagNo)) {
                System.out.println("[托盘码关联] 警告：触发箱码的TagNo(" + tagNo + ")与当前正在采集的TagNo(" + currentTagNo + ")不一致，可能是满垛后的关联操作");
            }
            
            // 3. 查询该TagNo下的实际采集箱数
            Long actualBoxCount = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            System.out.println("[托盘码关联] 实际采集箱数=" + actualBoxCount + ", 应采集箱数(Qty)=" + qty);
            
            // 4. 验证箱数是否匹配（除非已点击强制满垛）
            Boolean forcePalletFlag = forcePalletFlagMap.get(cacheKey);
            if (forcePalletFlag == null || !forcePalletFlag) {
                // 没有点击强制满垛，必须验证箱数
                if (!actualBoxCount.equals(qty.longValue())) {
                    String errorMsg = "关联失败：实际采集箱数(" + actualBoxCount + ")与应采集箱数(" + qty + ")不匹配！" +
                                    "如需忽略箱数差异，请先点击【强制满垛】按钮。";
                    System.err.println("[托盘码关联] " + errorMsg);
                    return ApiResult.error(errorMsg);
                }
            } else {
                System.out.println("[托盘码关联] 已启用强制满垛模式，忽略箱数差异检查");
            }
            
            // 查询该TagNo的最新Qty个记录
            Page<CodeRelationPO> page = new Page<>(1, qty);
            Page<CodeRelationPO> palletPage = codeRelationMapper.selectPage(page,
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .orderByDesc(CodeRelationPO::getAddTime)
            );
            
            List<CodeRelationPO> palletRecords = palletPage.getRecords();
            
            if (palletRecords.isEmpty()) {
                return ApiResult.error("未找到TagNo对应的箱码记录");
            }
            
            // 生成虚拟垛标
            String virtualPalletCode = generateVirtualPalletCode(palletCode);
            
            // 批量更新
            List<Integer> recordIds = palletRecords.stream()
                    .map(CodeRelationPO::getId)
                    .collect(Collectors.toList());
            
            LambdaUpdateWrapper<CodeRelationPO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(CodeRelationPO::getId, recordIds)
                         .set(CodeRelationPO::getBigSerialNumber, palletCode)
                         .set(CodeRelationPO::getVirtualSerialNumber, virtualPalletCode)
                         .set(CodeRelationPO::getIsVirtual, 1)
                         .set(CodeRelationPO::getIsUpload, 0);
            
            int updateCount = codeRelationMapper.update(null, updateWrapper);
            
            // 校验
            Long failedCount = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .in(CodeRelationPO::getId, recordIds)
                    .and(wrapper -> wrapper
                        .isNull(CodeRelationPO::getVirtualSerialNumber)
                        .or()
                        .eq(CodeRelationPO::getVirtualSerialNumber, ""))
            );
            
            if (failedCount > 0) {
                return ApiResult.error("托盘码关联失败：有 " + failedCount + " 条记录未成功写入虚拟垛标");
            }
            
            // 重新统计该垛中BigSerialNumber有值的数量，更新Qty字段
            Long actualPalletCount = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .isNotNull(CodeRelationPO::getBigSerialNumber)
                    .ne(CodeRelationPO::getBigSerialNumber, "")
            );
            
            // 将统计的数量更新到该垛的所有记录的Qty字段
            LambdaUpdateWrapper<CodeRelationPO> qtyUpdateWrapper = new LambdaUpdateWrapper<>();
            qtyUpdateWrapper.eq(CodeRelationPO::getTagNo, tagNo)
                           .eq(CodeRelationPO::getOrderNo, orderNo)
                           .eq(CodeRelationPO::getIsDel, 0)
                           .set(CodeRelationPO::getQty, actualPalletCount.intValue());
            
            codeRelationMapper.update(null, qtyUpdateWrapper);
            
            System.out.println("[托盘码关联] 成功：订单=" + orderNo 
                             + ", 触发箱码=" + triggerBoxCode
                             + ", TagNo=" + tagNo
                             + ", 托盘码=" + palletCode
                             + ", 虚拟垛标=" + virtualPalletCode
                             + ", 更新数量=" + updateCount
                             + ", 实际BigSerialNumber有值数量=" + actualPalletCount
                             + ", Qty字段已更新为=" + actualPalletCount);
            
            // 清除该垛的缓存，但要判断是否是当前正在采集的垛
            String currentlyCollectingTagNo = currentTagNoMap.get(cacheKey);
            
            if (currentlyCollectingTagNo != null && currentlyCollectingTagNo.equals(tagNo)) {
                // 如果当前正在采集的TagNo与关联的TagNo一致，说明是刚满垛就立即关联的情况
                // 此时才清除缓存
                currentTagNoMap.remove(cacheKey);
                palletCountMap.remove(tagNo);
                System.out.println("[托盘码关联] 已清除订单 " + orderNo + " 产品 " + productNo + " 的TagNo缓存（TagNo=" + tagNo + "）");
            } else if (currentlyCollectingTagNo != null) {
                // 如果不一致，说明相机已经开始采集下一垛了，不能清除当前垛的缓存
                System.out.println("[托盘码关联] 相机已经开始采集下一垛（新TagNo=" + currentlyCollectingTagNo + "），不清除缓存，仅清除已完成垛的计数（TagNo=" + tagNo + "）");
                // 只清除已完成垛的计数缓存
                palletCountMap.remove(tagNo);
            } else {
                // currentlyCollectingTagNo为null，说明缓存已经被清除过了（可能是满垛时清除的）
                System.out.println("[托盘码关联] 订单 " + orderNo + " 产品 " + productNo + " 的TagNo缓存已被清除，仅清除已完成垛的计数（TagNo=" + tagNo + "）");
                palletCountMap.remove(tagNo);
            }
            
            // 更新ProductionOrder表的OrderSumCount字段（累加已生成虚拟垛标的数量）
            updateOrderSumCount(orderNo, updateCount);
            
            // 更新ProductionOrderDetail表的OrderCount字段（累加已生成虚拟垛标的数量）
            updateOrderDetailCount(orderNo, productNo, updateCount);
            
            // 清除强制满垛标记（关联成功后重置）
            forcePalletFlagMap.remove(cacheKey);
            System.out.println("[托盘码关联] 已清除强制满垛标记（订单=" + orderNo + ", 产品=" + productNo + "）");
            
            return ApiResult.success("托盘码关联成功", 
                PalletAssociateResult.success(tagNo, palletCode, virtualPalletCode, updateCount));
        } catch (Exception e) {
            return ApiResult.error("托盘码关联失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成虚拟垛标
     */
    private String generateVirtualPalletCode(String palletCode) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomNum = String.format("%03d", new Random().nextInt(1000));
        return "#" + palletCode + "#_" + dateStr + randomNum + "C";
    }
    
    /**
     * 获取当前垛信息（通过taskId）
     * @param taskId 任务ID（ProductionOrderDetail.Id）
     */
    public ApiResult<CurrentPalletInfoVO> getCurrentPalletInfoByTaskId(Integer taskId) {
        try {
            if (taskId == null) {
                return ApiResult.error("任务ID不能为空");
            }
            
            // 根据taskId查询订单详情
            ProductionOrderDetailPO orderDetail = productionOrderDetailMapper.selectById(taskId);
            
            if (orderDetail == null) {
                return ApiResult.error("任务不存在");
            }
            
            String orderNo = orderDetail.getOrderNo();
            String productNo = orderDetail.getProductNo();
            String cacheKey = generateCacheKey(orderNo, productNo);
            String tagNo = currentTagNoMap.get(cacheKey);
            
            Integer totalCount = (orderDetail.getRatio() != null) ? orderDetail.getRatio() : 0;
            
            if (tagNo == null) {
                return ApiResult.success("查询成功", 
                    CurrentPalletInfoVO.create(orderNo, null, 0, totalCount));
            }
            
            // 从内存缓存获取当前箱数
            Integer currentCount = palletCountMap.getOrDefault(tagNo, 0);
            
            // 获取Type字段（1=有箱码，2=无箱码）
            Integer type = orderDetail.getType();
            
            // 查询CodeRelationUpload表中该订单、产品和TagNo的箱数
            // 无论有箱码还是无箱码模式，都使用统一的查询逻辑
            try {
                LambdaQueryWrapper<com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO> queryWrapper = 
                    new LambdaQueryWrapper<com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO>()
                        .eq(com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO::getOrderNo, orderNo)
                        .eq(com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO::getProductNo, productNo)
                        .eq(com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO::getTagNo, tagNo)
                        .eq(com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO::getIsDel, 0);
                
                // 无箱码模式需要额外过滤Type=2
                if (type != null && type == 2) {
                    queryWrapper.eq(com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO::getType, 2);
                }
                
                Long uploadCount = codeRelationUploadMapper.selectCount(queryWrapper);
                
                // 如果是无箱码模式，累加到内存箱数；如果是有箱码模式，直接使用数据库查询结果
                if (type != null && type == 2) {
                    currentCount += uploadCount.intValue();
                    System.out.println("[获取当前垛信息-无箱码] 订单=" + orderNo + ", 产品=" + productNo + ", TagNo=" + tagNo + 
                                     ", 内存箱数=" + palletCountMap.getOrDefault(tagNo, 0) + 
                                     ", CodeRelationUpload表箱数=" + uploadCount + 
                                     ", 总箱数=" + currentCount);
                } else {
                    currentCount = uploadCount.intValue();
                    System.out.println("[获取当前垛信息-有箱码] 订单=" + orderNo + ", 产品=" + productNo + ", TagNo=" + tagNo + 
                                     ", 当前箱数=" + currentCount);
                }
            } catch (Exception e) {
                System.err.println("[获取当前垛信息] 查询CodeRelationUpload表失败: " + e.getMessage());
                // 查询失败时，使用内存缓存的值
            }
            
            return ApiResult.success("查询成功", 
                CurrentPalletInfoVO.create(orderNo, tagNo, currentCount, totalCount));
        } catch (Exception e) {
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取当前垛信息（通过orderNo）
     * @deprecated 当订单有多个产品明细时，应使用getCurrentPalletInfoByTaskId
     */
    @Deprecated
    public ApiResult<CurrentPalletInfoVO> getCurrentPalletInfo(String orderNo) {
        try {
            if (!StringUtils.hasText(orderNo)) {
                return ApiResult.error("订单编号不能为空");
            }
            
            // 注意：这个方法已废弃，建议使用getCurrentPalletInfoByTaskId
            // 由于无法获取productNo，这里仍保留旧逻辑，但实际应该废弃不用
            String tagNo = currentTagNoMap.get(orderNo + "_UNKNOWN");
            
            // 查询生产订单详情获取采集规格（每垛箱数）
            // 一个订单可能有多个产品明细，取第一条
            Page<ProductionOrderDetailPO> page = new Page<>(1, 1);
            Page<ProductionOrderDetailPO> orderPage = productionOrderDetailMapper.selectPage(page,
                new LambdaQueryWrapper<ProductionOrderDetailPO>()
                    .eq(ProductionOrderDetailPO::getOrderNo, orderNo)
                    .eq(ProductionOrderDetailPO::getIsDel, 0)
            );
            List<ProductionOrderDetailPO> orderList = orderPage.getRecords();
            
            Integer totalCount = 0;
            if (orderList != null && !orderList.isEmpty()) {
                ProductionOrderDetailPO orderDetail = orderList.get(0);
                totalCount = (orderDetail.getRatio() != null) ? orderDetail.getRatio() : 0;
            }
            
            if (tagNo == null) {
                return ApiResult.success("查询成功", 
                    CurrentPalletInfoVO.create(orderNo, null, 0, totalCount));
            }
            
            Integer currentCount = palletCountMap.getOrDefault(tagNo, 0);
            
            return ApiResult.success("查询成功", 
                CurrentPalletInfoVO.create(orderNo, tagNo, currentCount, totalCount));
        } catch (Exception e) {
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 调整当前箱数
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized ApiResult<AdjustBoxCountResult> adjustBoxCount(AdjustBoxCountRequest request) {
        try {
            String orderNo = request.getOrderNo();
            String productNo = request.getProductNo();
            Integer targetCount = request.getTargetCount();
            Integer boxesPerPallet = request.getBoxesPerPallet();
            
            if (orderNo == null || orderNo.isEmpty()) {
                return ApiResult.error("订单编号不能为空");
            }
            if (productNo == null || productNo.isEmpty()) {
                return ApiResult.error("产品编号不能为空");
            }
            if (targetCount == null || targetCount < 0) {
                return ApiResult.error("目标箱数不能为空且必须大于等于0");
            }
            if (boxesPerPallet == null || boxesPerPallet <= 0) {
                return ApiResult.error("每垛箱数必须大于0");
            }
            // 新增校验：目标箱数不能大于每垛箱数
            if (targetCount > boxesPerPallet) {
                return ApiResult.error("目标箱数(" + targetCount + ")不能大于每垛箱数(" + boxesPerPallet + ")");
            }
            
            String cacheKey = generateCacheKey(orderNo, productNo);
            String tagNo = currentTagNoMap.get(cacheKey);
            if (tagNo == null || tagNo.isEmpty()) {
                return ApiResult.error("当前订单没有正在采集的垛，无法调整");
            }
            
            Integer currentCount = palletCountMap.getOrDefault(tagNo, 0);
            
            if (targetCount.equals(currentCount)) {
                return ApiResult.success("调整成功", 
                    AdjustBoxCountResult.success(tagNo, currentCount, currentCount, "NONE", 0, "当前箱数已是" + currentCount + "，无需调整"));
            } else if (targetCount < currentCount) {
                return deleteLatestBoxCodes(orderNo, productNo, tagNo, currentCount, targetCount, boxesPerPallet);
            } else {
                return addVirtualBoxCodes(orderNo, productNo, tagNo, currentCount, targetCount, boxesPerPallet);
            }
        } catch (Exception e) {
            return ApiResult.error("调整失败：" + e.getMessage());
        }
    }
    
    private ApiResult<AdjustBoxCountResult> deleteLatestBoxCodes(String orderNo, String productNo, String tagNo, 
                                                                  Integer currentCount, Integer targetCount, Integer boxesPerPallet) {
        int deleteCount = currentCount - targetCount;
        
        Page<CodeRelationPO> page = new Page<>(1, deleteCount);
        Page<CodeRelationPO> deletePage = codeRelationMapper.selectPage(page,
            new LambdaQueryWrapper<CodeRelationPO>()
                .eq(CodeRelationPO::getOrderNo, orderNo)
                .eq(CodeRelationPO::getTagNo, tagNo)
                .eq(CodeRelationPO::getIsDel, 0)
                .orderByDesc(CodeRelationPO::getAddTime)
        );
        
        List<CodeRelationPO> recordsToDelete = deletePage.getRecords();
        
        if (recordsToDelete.size() != deleteCount) {
            return ApiResult.error("数据异常：需要删除" + deleteCount + "条，但只查询到" + recordsToDelete.size() + "条");
        }
        
        List<Integer> idsToDelete = recordsToDelete.stream()
                .map(CodeRelationPO::getId)
                .collect(Collectors.toList());
        
        codeRelationMapper.update(null,
            new LambdaUpdateWrapper<CodeRelationPO>()
                .in(CodeRelationPO::getId, idsToDelete)
                .set(CodeRelationPO::getIsDel, 1)
        );
        
        palletCountMap.put(tagNo, targetCount);
        
        // 检查是否满垛，如果满垛则清除缓存
        String cacheKey = generateCacheKey(orderNo, productNo);
        if (targetCount >= boxesPerPallet) {
            System.out.println("[指定箱数-删除模式] 满垛！订单=" + orderNo + ", 产品=" + productNo + ", TagNo=" + tagNo + ", 目标箱数=" + targetCount);
            currentTagNoMap.remove(cacheKey);
            palletCountMap.remove(tagNo);
        }
        
        return ApiResult.success("调整成功", 
            AdjustBoxCountResult.success(tagNo, currentCount, targetCount, "DELETE", deleteCount, "成功删除" + deleteCount + "个最新采集的箱码"));
    }
    
    private ApiResult<AdjustBoxCountResult> addVirtualBoxCodes(String orderNo, String productNo, String tagNo, 
                                                                Integer currentCount, Integer targetCount, Integer boxesPerPallet) {
        int addCount = targetCount - currentCount;
        
        List<CodeRelationPO> records = codeRelationMapper.selectList(
            new LambdaQueryWrapper<CodeRelationPO>()
                .eq(CodeRelationPO::getOrderNo, orderNo)
                .eq(CodeRelationPO::getTagNo, tagNo)
                .eq(CodeRelationPO::getIsDel, 0)
        );
        
        if (records.isEmpty()) {
            return ApiResult.error("未找到当前垛的记录，无法生成虚拟箱码");
        }
        
        CodeRelationPO templateRecord = records.get(0);
        List<String> generatedCodes = new ArrayList<>();
        
        for (int i = 0; i < addCount; i++) {
            CodeRelationPO entity = new CodeRelationPO();
            String generatedCode = generateUniqueBoxCode(orderNo);
            generatedCodes.add(generatedCode); // 收集生成的虚拟箱码
            
            entity.setSmallSerialNumber(generatedCode);
            entity.setVirtualSerialNumber("");
            entity.setProductNo(templateRecord.getProductNo());
            entity.setOrderNo(orderNo);
            entity.setBatchNo(templateRecord.getBatchNo());
            entity.setTagNo(tagNo);
            entity.setType(templateRecord.getType());
            entity.setQty(boxesPerPallet); // 使用传入的采集规格箱数
            entity.setIsVirtual(0);
            entity.setStatus(0);
            entity.setIsUpload(1);
            entity.setIsDel(0);
            entity.setAddTime(LocalDateTime.now());
            entity.setErrCount(0);
            entity.setWarehouseNo(templateRecord.getWarehouseNo());
            entity.setTeamName(templateRecord.getTeamName());
            entity.setBigSerialNumber("");
            entity.setBiggerSerialNumber("");
            entity.setMediumSerialNumber("");
            entity.setDxCode("");
            entity.setSalesCode("");
            entity.setMsg("");
            
            codeRelationMapper.insert(entity);
        }
        
        palletCountMap.put(tagNo, targetCount);
        
        // 检查是否满垛，如果满垛则清除缓存
        String cacheKey = generateCacheKey(orderNo, productNo);
        if (targetCount >= boxesPerPallet) {
            System.out.println("[指定箱数-添加模式] 满垛！订单=" + orderNo + ", 产品=" + productNo + ", TagNo=" + tagNo + ", 目标箱数=" + targetCount);
            currentTagNoMap.remove(cacheKey);
            palletCountMap.remove(tagNo);
        }
        
        return ApiResult.success("调整成功", 
            AdjustBoxCountResult.success(tagNo, currentCount, targetCount, "ADD", addCount, "成功自动生成" + addCount + "个虚拟箱码", generatedCodes));
    }
    
    /**
     * 强制满垛
     * 新逻辑：不再生成虚拟箱码，只设置"允许箱数不匹配"的标记
     * 仍需通过设备扫码进行托盘码关联，但关联时可忽略箱数不匹配的限制
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized ApiResult<ForcePalletResult> forcePallet(ForcePalletRequest request) {
        try {
            String orderNo = request.getOrderNo();
            String productNo = request.getProductNo();
            Integer boxesPerPallet = request.getBoxesPerPallet();
            
            if (orderNo == null || orderNo.isEmpty()) {
                return ApiResult.error("订单编号不能为空");
            }
            
            if (productNo == null || productNo.isEmpty()) {
                return ApiResult.error("产品编号不能为空");
            }
            
            if (boxesPerPallet == null || boxesPerPallet <= 0) {
                return ApiResult.error("每垛箱数必须大于0");
            }
            
            String cacheKey = generateCacheKey(orderNo, productNo);
            String tagNo = currentTagNoMap.get(cacheKey);
            if (tagNo == null || tagNo.isEmpty()) {
                return ApiResult.error("当前订单产品没有正在采集的垛，无法强制满垛");
            }
            
            Integer currentCount = palletCountMap.getOrDefault(tagNo, 0);
            if (currentCount == 0) {
                return ApiResult.error("当前垛没有采集任何箱码，无法强制满垛");
            }
            
            // 查询当前垛的记录
            List<CodeRelationPO> records = codeRelationMapper.selectList(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            if (records.isEmpty()) {
                return ApiResult.error("未找到当前垛的记录，无法强制满垛");
            }
            
            System.out.println("[强制满垛] 订单=" + orderNo + ", 产品=" + productNo + ", TagNo=" + tagNo 
                             + ", 当前箱数=" + currentCount 
                             + ", 应采集箱数=" + boxesPerPallet);
            
            // 设置强制满垛标记，允许箱数不匹配
            forcePalletFlagMap.put(cacheKey, true);
            System.out.println("[强制满垛] 已设置强制满垛标记（订单=" + orderNo + ", 产品=" + productNo + "），允许箱数不匹配");
            
            // 标记该垛为满垛状态（清除缓存，等待托盘码关联）
            currentTagNoMap.remove(cacheKey);
            palletCountMap.remove(tagNo);
            
            // 如果是无箱码模式，将tagNo保存到待关联Map中
            if (!records.isEmpty() && records.get(0).getType() == 2) {
                pendingNoBoxTagNoMap.put(cacheKey, tagNo);
                System.out.println("[强制满垛-无箱码] 订单=" + orderNo + ", 产品=" + productNo + ", TagNo=" + tagNo + " 已保存到待关联Map");
            }
            
            String message = "强制满垛成功！当前采集了 " + currentCount + " 箱（应采集 " + boxesPerPallet + " 箱）。" +
                           "已标记为允许箱数不匹配，请通过设备扫码进行托盘码关联。";
            
            System.out.println("[强制满垛] " + message);
            
            return ApiResult.success("强制满垛成功", 
                ForcePalletResult.success(tagNo, currentCount, boxesPerPallet, 0, message, new ArrayList<>()));
        } catch (Exception e) {
            return ApiResult.error("强制满垛失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除本垛无码
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized ApiResult<DeleteEmptyCodesResult> deleteEmptyCodes(DeleteEmptyCodesRequest request) {
        try {
            String orderNo = request.getOrderNo();
            String productNo = request.getProductNo();
            
            if (orderNo == null || orderNo.isEmpty()) {
                return ApiResult.error("订单编号不能为空");
            }
            
            if (productNo == null || productNo.isEmpty()) {
                return ApiResult.error("产品编号不能为空");
            }
            
            String cacheKey = generateCacheKey(orderNo, productNo);
            String tagNo = currentTagNoMap.get(cacheKey);
            if (tagNo == null || tagNo.isEmpty()) {
                return ApiResult.error("当前订单产品没有正在采集的垛，无法删除无码");
            }
            
            Integer currentCount = palletCountMap.getOrDefault(tagNo, 0);
            
            List<CodeRelationPO> emptyCodeRecords = codeRelationMapper.selectList(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .apply("LEN(SmallSerialNumber) = 22")  // SQL Server 使用 LEN 函数
            );
            
            int deleteCount = emptyCodeRecords.size();
            
            if (deleteCount == 0) {
                return ApiResult.success("删除无码成功", 
                    DeleteEmptyCodesResult.success(tagNo, currentCount, 0, currentCount, "当前垛没有无码记录"));
            }
            
            List<Integer> idsToDelete = emptyCodeRecords.stream()
                    .map(CodeRelationPO::getId)
                    .collect(Collectors.toList());
            
            codeRelationMapper.update(null,
                new LambdaUpdateWrapper<CodeRelationPO>()
                    .in(CodeRelationPO::getId, idsToDelete)
                    .set(CodeRelationPO::getIsDel, 1)
            );
            
            // 从数据库查询删除后的实际箱数
            Long actualCountLong = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            int afterCount = actualCountLong.intValue();
            palletCountMap.put(tagNo, afterCount);
            
            String message = "成功删除 " + deleteCount + " 条无码记录，当前箱数从 " + currentCount + " 减少到 " + afterCount;
            return ApiResult.success("删除无码成功", 
                DeleteEmptyCodesResult.success(tagNo, currentCount, deleteCount, afterCount, message));
        } catch (Exception e) {
            return ApiResult.error("删除无码失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成系统箱码（手动添加）
     * 直接调用addCode方法生成一个系统箱码并插入数据库
     */
    @Transactional(rollbackFor = Exception.class)
    public synchronized ApiResult<GenerateSystemCodeResult> generateSystemCode(GenerateSystemCodeRequest request) {
        try {
            if (request.getOrderNo() == null || request.getOrderNo().isEmpty()) {
                return ApiResult.error("订单编号不能为空");
            }
            if (request.getBoxCount() == null || request.getBoxCount() <= 0) {
                return ApiResult.error("箱数量必须大于0");
            }
            
            // 构建addCode请求
            CodeAddRequest addRequest = new CodeAddRequest();
            addRequest.setSmallSerialNumber(null); // 不传箱码，让addCode生成
            addRequest.setOrderNo(request.getOrderNo());
            addRequest.setProductNo(request.getProductNo());
            addRequest.setBatchNo(request.getBatchNo());
            addRequest.setType(request.getType());
            addRequest.setBoxCount(request.getBoxCount());
            addRequest.setStackCount(request.getStackCount());
            
            // 调用addCode生成系统箱码
            ApiResult<GenerateSystemCodeResult> addResult = addCode(addRequest);
            
            if (addResult.getCode() == 200) {
                // 直接返回addCode的结果
                return addResult;
            } else {
                return ApiResult.error("生成系统箱码失败：" + addResult.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("生成系统箱码失败：" + e.getMessage());
        }
    }
    
    private String generateUniqueSystemCode(String orderNo) {
        for (int attempt = 0; attempt < 100; attempt++) {
            StringBuilder numberPart = new StringBuilder();
            Random random = new Random();
            for (int i = 0; i < 18; i++) {
                numberPart.append(random.nextInt(10));
            }
            
            String generatedCode = "pzmz" + numberPart.toString();
            
            Long count = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getSmallSerialNumber, generatedCode)
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            if (count == 0) {
                return generatedCode;
            }
        }
        
        throw new RuntimeException("生成唯一系统箱码失败，超过最大尝试次数");
    }
    
    /**
     * 无箱码托盘码关联
     * 更新当前订单最近采集的一垛无码数据，写入托盘码和虚拟垛标
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<NoBoxCollectResult> collectNoBox(NoBoxCollectRequest request) {
        try {
            String orderNo = request.getOrderNo();
            String productNo = request.getProductNo();
            String palletCode = request.getPalletCode();
            
            // 查找当前订单产品待关联的TagNo（从pendingNoBoxTagNoMap获取）
            String cacheKey = generateCacheKey(orderNo, productNo);
            String tagNo = pendingNoBoxTagNoMap.get(cacheKey);
            
            // 如果待关联Map中没有，尝试从当前采集Map中获取（可能是未满垛的情况）
            if (tagNo == null || tagNo.isEmpty()) {
                tagNo = currentTagNoMap.get(cacheKey);
                
                // 如果当前采集Map中也没有，说明没有任何采集数据
            if (tagNo == null || tagNo.isEmpty()) {
                return ApiResult.error("未找到待关联的无箱码数据，请先通过箱码采集设备采集");
            }
            
                System.out.println("[无箱码托盘码关联] 当前垛未满垛，从当前采集Map获取TagNo=" + tagNo + ", 订单=" + orderNo + ", 产品=" + productNo);
            } else {
            System.out.println("[无箱码托盘码关联] 从待关联Map获取TagNo=" + tagNo + ", 订单=" + orderNo + ", 产品=" + productNo);
            }
            
            // 从前端传递的参数获取每垛箱数（boxesPerPallet）- 应采箱数取前端值
            Integer qty = request.getBoxesPerPallet();
            
            if (qty == null || qty <= 0) {
                return ApiResult.error("每垛箱数（boxesPerPallet）必须大于0");
            }
            
            // 查询该TagNo下的所有无码记录（用于校验实际采箱数）
            Long actualBoxCount = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getType, 2) // 无箱码模式
                    .eq(CodeRelationPO::getIsDel, 0)
                    .and(wrapper -> wrapper
                        .isNull(CodeRelationPO::getBigSerialNumber)
                        .or()
                        .eq(CodeRelationPO::getBigSerialNumber, ""))
            );
            
            System.out.println("[无箱码托盘码关联] 实际采集箱数=" + actualBoxCount + ", 应采集箱数(Qty)=" + qty);
            
            // 验证箱数是否匹配（除非已点击强制满垛）- 与有箱码的校验逻辑保持一致
            Boolean forcePalletFlag = forcePalletFlagMap.get(cacheKey);
            if (forcePalletFlag == null || !forcePalletFlag) {
                // 没有点击强制满垛，必须验证箱数
                if (!actualBoxCount.equals(qty.longValue())) {
                    String errorMsg = "错误:关联失败:实际采集箱数(" + actualBoxCount + ")与应采集箱数(" + qty + ")不匹配!如需忽略箱数差异，请先点击【强制满垛】按钮。";
                    System.err.println("[无箱码托盘码关联] " + errorMsg);
                    return ApiResult.error(errorMsg);
                }
            } else {
                System.out.println("[无箱码托盘码关联] 已启用强制满垛模式，忽略箱数差异检查，将按照前端传递的应采箱数(" + qty + ")进行更新");
            }
            
            // 查询该TagNo的最新Qty个记录（按照前端传递的应采箱数，类似有箱码的逻辑）
            Page<CodeRelationPO> page = new Page<>(1, qty);
            Page<CodeRelationPO> palletPage = codeRelationMapper.selectPage(page,
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getType, 2) // 无箱码模式
                    .eq(CodeRelationPO::getIsDel, 0)
                    .and(wrapper -> wrapper
                        .isNull(CodeRelationPO::getBigSerialNumber)
                        .or()
                        .eq(CodeRelationPO::getBigSerialNumber, ""))
                    .orderByDesc(CodeRelationPO::getAddTime)
            );
            
            List<CodeRelationPO> palletRecords = palletPage.getRecords();
            
            if (palletRecords == null || palletRecords.isEmpty()) {
                return ApiResult.error("未找到待关联的无箱码记录");
            }
            
            // ProductNo已在方法开头从request获取
            System.out.println("[无箱码托盘码关联] 使用ProductNo: " + productNo);
            
            // 生成虚拟垛标
            String virtualPalletCode = generateVirtualPalletCode(palletCode);
            
            // 批量更新：写入托盘码和虚拟垛标（只更新最新的Qty条记录）
            List<Integer> recordIds = palletRecords.stream()
                .map(CodeRelationPO::getId)
                .collect(Collectors.toList());
            
            LambdaUpdateWrapper<CodeRelationPO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(CodeRelationPO::getId, recordIds)
                .set(CodeRelationPO::getBigSerialNumber, palletCode)
                .set(CodeRelationPO::getVirtualSerialNumber, virtualPalletCode)
                .set(CodeRelationPO::getIsVirtual, 1)
                .set(CodeRelationPO::getIsUpload, 0);
            
            int updateCount = codeRelationMapper.update(null, updateWrapper);
            
            // 校验更新是否成功（类似有箱码的逻辑）
            Long failedCount = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .in(CodeRelationPO::getId, recordIds)
                    .and(wrapper -> wrapper
                        .isNull(CodeRelationPO::getVirtualSerialNumber)
                        .or()
                        .eq(CodeRelationPO::getVirtualSerialNumber, ""))
            );
            
            if (failedCount > 0) {
                return ApiResult.error("托盘码关联失败：有 " + failedCount + " 条记录未成功写入虚拟垛标");
            }
            
            if (updateCount != palletRecords.size()) {
                return ApiResult.error("托盘码关联失败：期望更新 " + palletRecords.size() + " 条记录，实际更新 " + updateCount + " 条");
            }
            
            // 重新统计该垛中BigSerialNumber有值的数量，更新Qty字段
            Long actualPalletCount = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getTagNo, tagNo)
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .isNotNull(CodeRelationPO::getBigSerialNumber)
                    .ne(CodeRelationPO::getBigSerialNumber, "")
            );
            
            // 将统计的数量更新到该垛的所有记录的Qty字段
            LambdaUpdateWrapper<CodeRelationPO> qtyUpdateWrapper = new LambdaUpdateWrapper<>();
            qtyUpdateWrapper.eq(CodeRelationPO::getTagNo, tagNo)
                           .eq(CodeRelationPO::getOrderNo, orderNo)
                           .eq(CodeRelationPO::getIsDel, 0)
                           .set(CodeRelationPO::getQty, actualPalletCount.intValue());
            
            codeRelationMapper.update(null, qtyUpdateWrapper);
            
            // 清除待关联的TagNo（已完成托盘码关联）
            pendingNoBoxTagNoMap.remove(cacheKey);
            palletCountMap.remove(tagNo);
            
            System.out.println("[无箱码托盘码关联] 关联成功：订单=" + orderNo 
                             + ", TagNo=" + tagNo
                             + ", 托盘码=" + palletCode
                             + ", 虚拟垛标=" + virtualPalletCode
                             + ", 更新数量=" + updateCount
                             + "（按照前端传递的应采箱数=" + qty + "更新）"
                             + ", 实际BigSerialNumber有值数量=" + actualPalletCount 
                             + ", Qty字段已更新为=" + actualPalletCount);
            
            // 更新ProductionOrder表的OrderSumCount字段（累加已生成虚拟垛标的数量）
            updateOrderSumCount(orderNo, updateCount);
            
            // 更新ProductionOrderDetail表的OrderCount字段（累加已生成虚拟垛标的数量）
            updateOrderDetailCount(orderNo, productNo, updateCount);
            
            // 清除强制满垛标记（关联成功后重置）
            forcePalletFlagMap.remove(cacheKey);
            System.out.println("[无箱码托盘码关联] 已清除强制满垛标记（订单=" + orderNo + ", 产品=" + productNo + "）");
            
            String message = "无箱码托盘码关联成功，已更新 " + updateCount + " 条记录";
            return ApiResult.success("无箱码托盘码关联成功", 
                NoBoxCollectResult.success(message, updateCount, palletCode, virtualPalletCode, tagNo));
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("无箱码托盘码关联失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取订单已采集箱数
     * 查询CodeRelationUpload表中当前OrderNo的IsDel=0的记录数
     */
    public ApiResult<Integer> getCollectedCount(String orderNo) {
        try {
            if (!StringUtils.hasText(orderNo)) {
                return ApiResult.error("订单编号不能为空");
            }
            
            Long count = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            return ApiResult.success("查询成功", count.intValue());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取订单已生产垛数
     * 查询CodeRelationUpload表中该订单BigSerialNumber有值的记录中不同TagNo的数量
     */
    public ApiResult<Integer> getProducedPalletCount(String orderNo) {
        try {
            if (!StringUtils.hasText(orderNo)) {
                return ApiResult.error("订单编号不能为空");
            }
            
            // 直接查询所有TagNo，使用HashSet去重后计数
            // 只查询TagNo字段，减少数据传输量
            List<CodeRelationPO> records = codeRelationMapper.selectList(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .select(CodeRelationPO::getTagNo)
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .isNotNull(CodeRelationPO::getBigSerialNumber)
                    .ne(CodeRelationPO::getBigSerialNumber, "")
            );
            
            // 使用HashSet去重统计不同TagNo的数量
            java.util.Set<String> uniqueTagNos = new java.util.HashSet<>();
            for (CodeRelationPO record : records) {
                if (record.getTagNo() != null) {
                    uniqueTagNos.add(record.getTagNo());
                }
            }
            
            int palletCount = uniqueTagNos.size();
            
            System.out.println("[已生产垛数] 订单=" + orderNo + ", 垛数=" + palletCount);
            
            return ApiResult.success("查询成功", palletCount);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取产品已采集箱数（按OrderNo和ProductNo统计）
     * 查询CodeRelationUpload表中指定订单和产品的IsDel=0的记录数
     * 用于主界面单位实时统计，因为一个订单可能包含多个产品
     * 
     * @param orderNo 订单编号
     * @param productNo 产品编号
     * @return 已采集箱数
     */
    public ApiResult<Integer> getCollectedCountByProduct(String orderNo, String productNo) {
        try {
            if (!StringUtils.hasText(orderNo)) {
                return ApiResult.error("订单编号不能为空");
            }
            if (!StringUtils.hasText(productNo)) {
                return ApiResult.error("产品编号不能为空");
            }
            
            // 按OrderNo和ProductNo统计
            Long count = codeRelationMapper.selectCount(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getProductNo, productNo)
                    .eq(CodeRelationPO::getIsDel, 0)
            );
            
            return ApiResult.success("查询成功", count.intValue());
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取产品已生产垛数（按OrderNo和ProductNo统计）
     * 查询CodeRelationUpload表中指定订单和产品的BigSerialNumber有值的记录中不同TagNo的数量
     * 用于主界面单位实时统计，因为一个订单可能包含多个产品
     * 
     * 注意：此方法保留用于兼容性，新代码请使用 getProducedPalletCountByProductOptimized
     * 
     * @param orderNo 订单编号
     * @param productNo 产品编号
     * @return 已生产垛数
     */
    public ApiResult<Integer> getProducedPalletCountByProduct(String orderNo, String productNo) {
        try {
            if (!StringUtils.hasText(orderNo)) {
                return ApiResult.error("订单编号不能为空");
            }
            if (!StringUtils.hasText(productNo)) {
                return ApiResult.error("产品编号不能为空");
            }
            
            // 直接查询不同TagNo的数量，使用HashSet去重后计数
            // 只查询TagNo字段，减少数据传输量
            List<CodeRelationPO> records = codeRelationMapper.selectList(
                new LambdaQueryWrapper<CodeRelationPO>()
                    .select(CodeRelationPO::getTagNo)
                    .eq(CodeRelationPO::getOrderNo, orderNo)
                    .eq(CodeRelationPO::getProductNo, productNo)
                    .eq(CodeRelationPO::getIsDel, 0)
                    .isNotNull(CodeRelationPO::getBigSerialNumber)
                    .ne(CodeRelationPO::getBigSerialNumber, "")
            );
            
            // 使用HashSet去重统计不同TagNo的数量
            java.util.Set<String> uniqueTagNos = new java.util.HashSet<>();
            for (CodeRelationPO record : records) {
                if (record.getTagNo() != null) {
                    uniqueTagNos.add(record.getTagNo());
                }
            }
            
            int palletCount = uniqueTagNos.size();
            
            System.out.println("[已生产垛数] 订单=" + orderNo + ", 产品=" + productNo + ", 垛数=" + palletCount);
            
            return ApiResult.success("查询成功", palletCount);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取产品已生产垛数（优化版，按OrderNo和ProductNo统计）
     * 使用COUNT(DISTINCT TagNo)在数据库层面直接计数，性能更优
     * 用于主界面单位实时统计，因为一个订单可能包含多个产品
     * 
     * @param orderNo 订单编号
     * @param productNo 产品编号
     * @return 已生产垛数
     */
    public ApiResult<Integer> getProducedPalletCountByProductOptimized(String orderNo, String productNo) {
        try {
            if (!StringUtils.hasText(orderNo)) {
                return ApiResult.error("订单编号不能为空");
            }
            if (!StringUtils.hasText(productNo)) {
                return ApiResult.error("产品编号不能为空");
            }
            
            // 使用COUNT(DISTINCT TagNo)在数据库层面直接计数，避免查询大量数据后在内存中去重
            Long count = codeRelationMapper.countDistinctTagNoByOrderAndProduct(orderNo, productNo);
            
            int palletCount = count != null ? count.intValue() : 0;
            
            System.out.println("[已生产垛数-优化] 订单=" + orderNo + ", 产品=" + productNo + ", 垛数=" + palletCount);
            
            return ApiResult.success("查询成功", palletCount);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新ProductionOrder表的OrderSumCount字段
     * 累加已生成虚拟垛标的数量
     * 
     * @param orderNo 订单编号
     * @param incrementCount 本次增加的数量
     */
    private void updateOrderSumCount(String orderNo, int incrementCount) {
        try {
            // 查询当前订单
            ProductionOrderPO order = productionOrderMapper.selectOne(
                new LambdaQueryWrapper<ProductionOrderPO>()
                    .eq(ProductionOrderPO::getOrderNo, orderNo)
            );
            
            if (order != null) {
                // 获取当前OrderSumCount值（可能为null）
                Integer currentCount = order.getOrderSumCount();
                if (currentCount == null) {
                    currentCount = 0;
                }
                
                // 累加
                int newCount = currentCount + incrementCount;
                
                // 更新
                LambdaUpdateWrapper<ProductionOrderPO> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(ProductionOrderPO::getOrderNo, orderNo)
                    .set(ProductionOrderPO::getOrderSumCount, newCount);
                
                int updateResult = productionOrderMapper.update(null, updateWrapper);
                
                System.out.println("[更新OrderSumCount] 订单=" + orderNo 
                                 + ", 原值=" + currentCount 
                                 + ", 增量=" + incrementCount 
                                 + ", 新值=" + newCount 
                                 + ", 更新结果=" + updateResult);
            } else {
                System.err.println("[更新OrderSumCount] 未找到订单：" + orderNo);
            }
        } catch (Exception e) {
            System.err.println("[更新OrderSumCount] 失败：订单=" + orderNo + ", 错误=" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 更新ProductionOrderDetail表的OrderCount字段
     * 累加已生成虚拟垛标的数量
     * 
     * @param orderNo 订单编号
     * @param productNo 产品编号
     * @param incrementCount 本次增加的数量
     */
    private void updateOrderDetailCount(String orderNo, String productNo, int incrementCount) {
        try {
            if (!StringUtils.hasText(productNo)) {
                System.err.println("[更新OrderCount] ProductNO为空，无法更新OrderCount");
                return;
            }
            
            // 查询当前订单详情（根据OrderNo和ProductNO）
            Page<ProductionOrderDetailPO> page = new Page<>(1, 1);
            Page<ProductionOrderDetailPO> orderDetailPage = productionOrderDetailMapper.selectPage(page,
                new LambdaQueryWrapper<ProductionOrderDetailPO>()
                    .eq(ProductionOrderDetailPO::getOrderNo, orderNo)
                    .eq(ProductionOrderDetailPO::getProductNo, productNo)
                    .eq(ProductionOrderDetailPO::getIsDel, 0)
            );
            
            List<ProductionOrderDetailPO> orderDetails = orderDetailPage.getRecords();
            
            if (orderDetails != null && !orderDetails.isEmpty()) {
                ProductionOrderDetailPO orderDetail = orderDetails.get(0);
                
                // 获取当前OrderCount值（可能为null）
                Integer currentCount = orderDetail.getOrderCount();
                if (currentCount == null) {
                    currentCount = 0;
                }
                
                // 累加
                int newCount = currentCount + incrementCount;
                
                // 更新
                LambdaUpdateWrapper<ProductionOrderDetailPO> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(ProductionOrderDetailPO::getOrderNo, orderNo)
                    .eq(ProductionOrderDetailPO::getProductNo, productNo)
                    .eq(ProductionOrderDetailPO::getIsDel, 0)
                    .set(ProductionOrderDetailPO::getOrderCount, newCount);
                
                int updateResult = productionOrderDetailMapper.update(null, updateWrapper);
                
                System.out.println("[更新OrderCount] 订单=" + orderNo 
                                 + ", 产品=" + productNo
                                 + ", 原值=" + currentCount 
                                 + ", 增量=" + incrementCount 
                                 + ", 新值=" + newCount 
                                 + ", 更新结果=" + updateResult);
            } else {
                System.err.println("[更新OrderCount] 未找到订单详情：OrderNo=" + orderNo + ", ProductNO=" + productNo);
            }
        } catch (Exception e) {
            System.err.println("[更新OrderCount] 失败：订单=" + orderNo + ", 产品=" + productNo + ", 错误=" + e.getMessage());
            e.printStackTrace();
        }
    }
}
