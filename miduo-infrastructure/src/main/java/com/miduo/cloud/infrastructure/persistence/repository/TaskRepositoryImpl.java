package com.miduo.cloud.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.common.dto.PageInput;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.domain.task.model.Task;
import com.miduo.cloud.domain.task.repository.TaskRepository;
import com.miduo.cloud.infrastructure.persistence.mybatis.mapper.ProductionOrderDetailMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.mapper.ProductionOrderMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.ProductionOrderDetailPO;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.ProductionOrderPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务仓储实现类
 * 实现领域层定义的TaskRepository接口
 * Task领域模型映射到 ProductionOrderDetail 表
 */
@Repository
public class TaskRepositoryImpl implements TaskRepository {
    
    @Autowired
    private ProductionOrderDetailMapper detailMapper;
    
    @Autowired
    private ProductionOrderMapper orderMapper;
    
    @Override
    public String save(Task task) {
        // 1. 生成订单号（格式：PO+yyyyMMdd+随机数）
        String orderNo = generateOrderNo();
        
        // 2. 保存 ProductionOrder（主表）
        ProductionOrderPO orderPO = new ProductionOrderPO();
        orderPO.setOrderNo(orderNo);
        orderPO.setType(task.getType());
        orderPO.setOrderStatus(task.getOrderStatus() != null ? task.getOrderStatus() : 0);
        orderPO.setOrderSumCount(0);  // 新增任务时OrderSumCount初始化为0
        orderPO.setProductSumCount(task.getProductSumCount());
        orderPO.setDmakeDate(task.getDmakeDate());
        orderPO.setCreateTime(LocalDateTime.now());
        
        // 设置默认值（与原代码保持一致）
        orderPO.setSrcId(0);                    // 默认为0
        orderPO.setProductSourceType(2);        // 设为2（产线）
        orderPO.setProductionLine(1);           // 暂时设为1
        
        orderMapper.insert(orderPO);
        
        // 3. 保存 ProductionOrderDetail（明细表）
        ProductionOrderDetailPO detailPO = convertToPO(task);
        detailPO.setOrderNo(orderNo);
        detailPO.setOrderId(orderPO.getId());
        detailPO.setCreateTime(LocalDateTime.now());
        detailPO.setAddTime(LocalDateTime.now());
        detailPO.setIsDel(0);
        detailMapper.insert(detailPO);
        
        return orderNo;
    }
    
    @Override
    public Optional<Task> findById(Integer id) {
        ProductionOrderDetailPO po = detailMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToModel);
    }
    
    @Override
    public Optional<Task> findByOrderNo(String orderNo) {
        Page<ProductionOrderDetailPO> page = new Page<>(1, 1);
        QueryWrapper<ProductionOrderDetailPO> wrapper = new QueryWrapper<>();
        wrapper.eq("OrderNo", orderNo)
               .eq("IsDel", 0)
               .orderByDesc("Id");
        
        Page<ProductionOrderDetailPO> result = detailMapper.selectPage(page, wrapper);
        if (result.getRecords().isEmpty()) {
            return Optional.empty();
        }
        ProductionOrderDetailPO po = result.getRecords().get(0);
        return Optional.ofNullable(po).map(this::convertToModel);
    }
    
    @Override
    public PageOutput<Task> findByPage(PageInput pageInput, String orderNo, Integer type) {
        // 构建分页对象
        Page<ProductionOrderDetailPO> page = new Page<>(pageInput.getCurrent(), pageInput.getSize());
        
        // 构建查询条件
        QueryWrapper<ProductionOrderDetailPO> wrapper = new QueryWrapper<>();
        wrapper.eq("IsDel", 0);
        
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like("OrderNo", orderNo);
        }
        if (type != null) {
            wrapper.eq("Type", type);
        }
        
        wrapper.orderByDesc("Id");
        
        // 执行分页查询
        IPage<ProductionOrderDetailPO> pageResult = detailMapper.selectPage(page, wrapper);
        
        // 转换为 PageOutput
        PageOutput<Task> output = new PageOutput<>();
        output.setCurrent(pageResult.getCurrent());
        output.setSize(pageResult.getSize());
        output.setTotal(pageResult.getTotal());
        output.setPages(pageResult.getPages());
        
        List<Task> tasks = pageResult.getRecords().stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
        output.setRecords(tasks);
        
        return output;
    }
    
    @Override
    public PageOutput<Task> findByPageWithFilters(PageInput pageInput, String orderNo, 
                                                   String syBatchNo, String productName, Integer orderStatus,
                                                   LocalDateTime productTimeStart, LocalDateTime productTimeEnd) {
        // 构建分页对象
        Page<ProductionOrderDetailPO> page = new Page<>(pageInput.getCurrent(), pageInput.getSize());
        
        // 构建查询条件
        QueryWrapper<ProductionOrderDetailPO> wrapper = new QueryWrapper<>();
        wrapper.eq("IsDel", 0);
        
        // 订单号模糊查询
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like("OrderNo", orderNo);
        }
        
        // 生产批次模糊查询
        if (syBatchNo != null && !syBatchNo.isEmpty()) {
            wrapper.like("SyBatchNo", syBatchNo);
        }
        
        // 产品名称模糊查询
        if (productName != null && !productName.isEmpty()) {
            wrapper.like("ProductName", productName);
        }
        
        // 预计生产时间范围查询（ProductTime字段）
        if (productTimeStart != null) {
            wrapper.ge("ProductTime", productTimeStart);
        }
        if (productTimeEnd != null) {
            // 结束时间设置为当天的23:59:59，以包含整个结束日期
            LocalDateTime endDateTime = productTimeEnd.toLocalDate().atTime(23, 59, 59);
            wrapper.le("ProductTime", endDateTime);
        }
        
        wrapper.orderByDesc("Id");
        
        // 执行分页查询
        IPage<ProductionOrderDetailPO> pageResult = detailMapper.selectPage(page, wrapper);
        
        // 转换为领域模型
        List<Task> tasks = pageResult.getRecords().stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
        
        // 如果需要按订单状态筛选，需要在转换后过滤（因为状态在ProductionOrder表）
        if (orderStatus != null) {
            tasks = tasks.stream()
                    .filter(task -> task.getOrderStatus() != null && task.getOrderStatus().equals(orderStatus))
                    .collect(Collectors.toList());
        }
        
        // 构建分页结果（注意：如果按orderStatus过滤，total和pages可能不准确）
        PageOutput<Task> output = new PageOutput<>();
        output.setCurrent(pageResult.getCurrent());
        output.setSize(pageResult.getSize());
        
        if (orderStatus != null) {
            // 按状态过滤后，需要重新计算总数和总页数
            output.setTotal((long) tasks.size());
            output.setPages((long) Math.ceil((double) tasks.size() / pageInput.getSize()));
        } else {
            output.setTotal(pageResult.getTotal());
            output.setPages(pageResult.getPages());
        }
        
        output.setRecords(tasks);
        
        return output;
    }
    
    @Override
    public PageOutput<Task> findByPageWithStatuses(PageInput pageInput, String orderNo,
                                                    String syBatchNo, String productName, List<Integer> orderStatuses,
                                                    LocalDateTime productTimeStart, LocalDateTime productTimeEnd) {
        // 构建分页对象
        Page<ProductionOrderDetailPO> page = new Page<>(pageInput.getCurrent(), pageInput.getSize());
        
        // 处理结束时间：设置为当天的23:59:59，以包含整个结束日期
        LocalDateTime endDateTime = null;
        if (productTimeEnd != null) {
            endDateTime = productTimeEnd.toLocalDate().atTime(23, 59, 59);
        }
        
        // 使用自定义Mapper方法，直接在SQL层面关联查询并过滤指定状态的订单
        IPage<ProductionOrderDetailPO> pageResult = detailMapper.selectOrdersByStatusesPage(
            page,
            orderNo != null && !orderNo.isEmpty() ? orderNo : null,
            syBatchNo != null && !syBatchNo.isEmpty() ? syBatchNo : null,
            productName != null && !productName.isEmpty() ? productName : null,
            orderStatuses,
            productTimeStart,
            endDateTime
        );
        
        // 转换为领域模型（由于SQL已经过滤了状态，这里不需要再次过滤）
        List<Task> tasks = pageResult.getRecords().stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
        
        // 构建分页结果
        PageOutput<Task> output = new PageOutput<>();
        output.setCurrent(pageResult.getCurrent());
        output.setSize(pageResult.getSize());
        output.setTotal(pageResult.getTotal());
        output.setPages(pageResult.getPages());
        output.setRecords(tasks);
        
        return output;
    }
    
    @Override
    public boolean update(Task task) {
        if (task.getId() == null) {
            return false;
        }
        ProductionOrderDetailPO po = convertToPO(task);
        return detailMapper.updateById(po) > 0;
    }
    
    @Override
    public boolean deleteById(Integer id) {
        UpdateWrapper<ProductionOrderDetailPO> wrapper = new UpdateWrapper<>();
        wrapper.eq("Id", id).set("IsDel", 1);
        return detailMapper.update(null, wrapper) > 0;
    }
    
    @Override
    public boolean updateStatus(Integer id, Integer status) {
        // 更新 ProductionOrderDetail 对应的 ProductionOrder 状态
        ProductionOrderDetailPO detailPO = detailMapper.selectById(id);
        if (detailPO == null || detailPO.getOrderNo() == null) {
            System.err.println("[更新任务状态] ProductionOrderDetail不存在或OrderNo为空，id=" + id);
            return false;
        }
        
        System.out.println("[更新任务状态] DetailId=" + id + ", OrderNo=" + detailPO.getOrderNo() + ", Status=" + status);
        
        // 使用OrderNo而不是OrderId来更新，因为OrderId可能与ProductionOrder.Id不一致
        UpdateWrapper<ProductionOrderPO> wrapper = new UpdateWrapper<>();
        wrapper.eq("OrderNo", detailPO.getOrderNo()).set("OrderStatus", status);
        
        int updateCount = orderMapper.update(null, wrapper);
        System.out.println("[更新任务状态] 更新结果: " + updateCount + " 行");
        
        if (updateCount == 0) {
            System.err.println("[更新任务状态] 警告：未找到OrderNo=" + detailPO.getOrderNo() + " 的ProductionOrder记录");
            // 如果没有对应的ProductionOrder记录，尝试创建一个
            ProductionOrderPO orderPO = orderMapper.selectOne(
                new QueryWrapper<ProductionOrderPO>().eq("OrderNo", detailPO.getOrderNo())
            );
            if (orderPO == null) {
                System.err.println("[更新任务状态] ProductionOrder记录不存在，需要先创建该订单");
            }
        }
        
        return updateCount > 0;
    }
    
    @Override
    public List<Task> findAll() {
        QueryWrapper<ProductionOrderDetailPO> wrapper = new QueryWrapper<>();
        wrapper.eq("IsDel", 0)
               .orderByDesc("Id");
        
        List<ProductionOrderDetailPO> poList = detailMapper.selectList(wrapper);
        return poList.stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }
    
    /**
     * 生成订单号（格式：PO+yyyyMMdd+4位随机数）
     */
    private String generateOrderNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = LocalDateTime.now().format(formatter);
        java.util.Random random = new java.util.Random();
        int randomNum = random.nextInt(9000) + 1000; // 生成1000-9999的随机数
        return "PO" + dateStr + randomNum;
    }
    
    /**
     * 将领域模型转换为持久化对象
     */
    private ProductionOrderDetailPO convertToPO(Task model) {
        if (model == null) {
            return null;
        }
        
        ProductionOrderDetailPO po = new ProductionOrderDetailPO();
        po.setId(model.getId());
        po.setOrderNo(model.getOrderNo());
        po.setType(model.getType());
        po.setOrderCount(model.getOrderCount());
        po.setProductCount(model.getProductCount());
        po.setProductId(model.getProductId());
        po.setProductName(model.getProductName());
        po.setProductNo(model.getProductNo());
        po.setProductFormatId(model.getProductFormatId());
        po.setProductFormatName(model.getProductFormatName());
        po.setSyBatchNo(model.getSyBatchNo());
        po.setRatio(model.getRatio());
        po.setProductTime(model.getProductTime());
        po.setTwillendTime(model.getTwillendTime());
        po.setIsDel(model.getIsDel());
        
        return po;
    }
    
    /**
     * 将持久化对象转换为领域模型
     */
    private Task convertToModel(ProductionOrderDetailPO po) {
        if (po == null) {
            return null;
        }
        
        Task model = new Task();
        model.setId(po.getId());
        model.setOrderNo(po.getOrderNo());
        model.setType(po.getType());
        model.setProductCount(po.getProductCount());
        model.setProductId(po.getProductId());
        model.setProductName(po.getProductName());
        model.setProductNo(po.getProductNo());
        model.setProductFormatId(po.getProductFormatId());
        model.setProductFormatName(po.getProductFormatName());
        model.setSyBatchNo(po.getSyBatchNo());
        model.setRatio(po.getRatio());
        model.setProductTime(po.getProductTime());
        model.setTwillendTime(po.getTwillendTime());
        model.setCreateTime(po.getCreateTime());
        model.setIsDel(po.getIsDel());
        model.setDmakeDate(po.getCreateTime()); // 使用创建时间作为制单日期
        
        // 使用ProductionOrderDetail表的OrderCount字段作为完成数量（与任务管理列表、数据上传页面保持一致）
        model.setOrderCount(po.getOrderCount() != null ? po.getOrderCount() : 0);
        
        // 查询关联的 ProductionOrder 获取状态等信息
        if (po.getOrderNo() != null) {
            QueryWrapper<ProductionOrderPO> wrapper = new QueryWrapper<>();
            wrapper.eq("OrderNo", po.getOrderNo());
            ProductionOrderPO orderPO = orderMapper.selectOne(wrapper);
            if (orderPO != null) {
                model.setOrderStatus(orderPO.getOrderStatus());
                model.setOrderSumCount(orderPO.getOrderSumCount());
                model.setProductSumCount(orderPO.getProductSumCount());
                model.setDmakeDate(orderPO.getDmakeDate());
            }
        }
        
        return model;
    }
}

