package com.miduo.cloud.application.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.application.device.DeviceApplicationService;
import com.miduo.cloud.application.task.mapper.TaskDtoMapper;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageInput;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.domain.task.model.Task;
import com.miduo.cloud.domain.task.repository.TaskRepository;
import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import com.miduo.cloud.entity.dto.task.TaskAddRequest;
import com.miduo.cloud.entity.dto.task.TaskQueryRequest;
import com.miduo.cloud.entity.dto.task.TaskUpdateRequest;
import com.miduo.cloud.entity.dto.task.TaskVO;
import com.miduo.cloud.infrastructure.persistence.mybatis.mapper.CodeRelationMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.mapper.ProductionOrderDetailMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeRelationPO;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.ProductionOrderDetailPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务应用服务
 * 负责任务相关用例的编排和事务管理
 */
@Service
public class TaskApplicationService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private CodeRelationMapper codeRelationMapper;
    
    @Autowired
    private ProductionOrderDetailMapper productionOrderDetailMapper;
    
    @Autowired
    private DeviceApplicationService deviceApplicationService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 添加任务用例
     * @param request 任务添加请求
     * @return 生成的订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<String> addTask(TaskAddRequest request) {
        try {
            // 1. DTO → Domain Model
            Task task = TaskDtoMapper.toDomain(request);
            
            // 2. 业务验证
            if (!task.validate()) {
                return ApiResult.error("任务数据验证失败");
            }
            
            // 3. 保存任务（Repository会生成orderNo）
            String orderNo = taskRepository.save(task);
            
            return ApiResult.success("任务创建成功", orderNo);
            
        } catch (Exception e) {
            return ApiResult.error("任务创建失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新任务用例
     * @param id 任务ID
     * @param request 更新请求
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Boolean> updateTask(Integer id, TaskUpdateRequest request) {
        try {
            // 1. 查询现有任务
            Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
            
            // 2. 更新Domain Model
            TaskDtoMapper.updateDomain(task, request);
            
            // 3. 验证并保存
            if (!task.validate()) {
                return ApiResult.error("更新数据验证失败");
            }
            
            boolean success = taskRepository.update(task);
            
            return success ? ApiResult.success(true) 
                          : ApiResult.error("任务更新失败");
            
        } catch (Exception e) {
            return ApiResult.error("任务更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除任务用例（逻辑删除）
     * @param id 任务ID
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Boolean> deleteTask(Integer id) {
        try {
            boolean success = taskRepository.deleteById(id);
            
            return success ? ApiResult.success(true)
                          : ApiResult.error("任务删除失败");
            
        } catch (Exception e) {
            return ApiResult.error("任务删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询任务用例（新增：支持TaskQueryRequest）
     * @param request 查询请求（包含分页参数和查询条件）
     * @return 分页结果
     */
    public ApiResult<PageOutput<TaskVO>> queryTaskPage(TaskQueryRequest request) {
        try {
            // 1. 构建分页参数
            PageInput pageInput = new PageInput();
            pageInput.setCurrent(Long.valueOf(request.getPageNum()));
            pageInput.setSize(Long.valueOf(request.getPageSize()));
            
            // 2. 调用支持更多筛选条件的查询方法
            PageOutput<Task> taskPage = taskRepository.findByPageWithFilters(
                pageInput, 
                request.getOrderNo(),
                request.getSyBatchNo(),
                request.getProductName(),
                request.getOrderStatus(),
                request.getProductTimeStart(),
                request.getProductTimeEnd()
            );
            
            // 3. Domain Model → VO
            List<TaskVO> voList = taskPage.getRecords().stream()
                .map(TaskDtoMapper::toVO)
                .collect(Collectors.toList());
            
            // 4. 构建分页结果
            PageOutput<TaskVO> result = new PageOutput<>();
            result.setCurrent(taskPage.getCurrent());
            result.setSize(taskPage.getSize());
            result.setTotal(taskPage.getTotal());
            result.setPages(taskPage.getPages());
            result.setRecords(voList);
            
            return ApiResult.success(result);
            
        } catch (Exception e) {
            return ApiResult.error("查询任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询任务用例（原有方法保留）
     * @param pageInput 分页参数
     * @param orderNo 订单号（可选）
     * @param type 类型（可选）
     * @return 分页结果
     */
    public ApiResult<PageOutput<TaskVO>> queryTaskPage(PageInput pageInput, String orderNo, Integer type) {
        try {
            // 1. 查询Domain Model
            PageOutput<Task> taskPage = taskRepository.findByPage(pageInput, orderNo, type);
            
            // 2. Domain Model → VO
            List<TaskVO> voList = taskPage.getRecords().stream()
                .map(TaskDtoMapper::toVO)
                .collect(Collectors.toList());
            
            // 3. 构建分页结果
            PageOutput<TaskVO> result = new PageOutput<>();
            result.setCurrent(taskPage.getCurrent());
            result.setSize(taskPage.getSize());
            result.setTotal(taskPage.getTotal());
            result.setPages(taskPage.getPages());
            result.setRecords(voList);
            
            return ApiResult.success(result);
            
        } catch (Exception e) {
            return ApiResult.error("查询任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据订单号查询任务信息（新增）
     * 用于主界面选择订单后自动更新当前生产信息
     * @param orderNo 生产订单号
     * @return 任务信息
     */
    public ApiResult<TaskVO> getTaskByOrderNo(String orderNo) {
        try {
            // 1. 查询Domain Model
            Task task = taskRepository.findByOrderNo(orderNo).orElse(null);
            
            if (task == null) {
                return ApiResult.error("未找到订单号为【" + orderNo + "】的任务");
            }
            
            // 2. Domain Model → VO
            TaskVO taskVO = TaskDtoMapper.toVO(task);
            
            return ApiResult.success("查询成功", taskVO);
            
        } catch (Exception e) {
            return ApiResult.error("任务查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询可选择的任务（排除已完成状态）
     * 专门用于主界面的"选择生产订单"对话框
     * 自动过滤状态为"已完成"(OrderStatus=2)的任务
     * 
     * @param request 查询请求（包含分页参数和查询条件）
     * @return 分页结果
     */
    public ApiResult<PageOutput<TaskVO>> querySelectableTaskPage(TaskQueryRequest request) {
        try {
            // 1. 构建分页参数
            PageInput pageInput = new PageInput();
            pageInput.setCurrent(Long.valueOf(request.getPageNum()));
            pageInput.setSize(Long.valueOf(request.getPageSize()));
            
            // 2. 如果用户明确请求查询已完成状态，则忽略该请求，强制过滤已完成状态
            // 如果用户指定了其他状态（0待生产，1生产中），保留该筛选
            Integer orderStatus = request.getOrderStatus();
            if (orderStatus != null && orderStatus == 2) {
                // 用户请求查询已完成状态，改为不限状态，但后续会排除已完成
                orderStatus = null;
            }
            
            // 3. 调用支持更多筛选条件的查询方法
            PageOutput<Task> taskPage = taskRepository.findByPageWithFilters(
                pageInput, 
                request.getOrderNo(),
                request.getSyBatchNo(),
                request.getProductName(),
                orderStatus,
                request.getProductTimeStart(),
                request.getProductTimeEnd()
            );
            
            // 4. 过滤掉已完成状态的任务（OrderStatus=2）
            List<TaskVO> voList = taskPage.getRecords().stream()
                .filter(task -> task.getOrderStatus() == null || task.getOrderStatus() != 2)
                .map(TaskDtoMapper::toVO)
                .collect(Collectors.toList());
            
            // 5. 构建分页结果（注意：过滤后总数可能会变化，但这里保持原有总数）
            PageOutput<TaskVO> result = new PageOutput<>();
            result.setCurrent(taskPage.getCurrent());
            result.setSize(taskPage.getSize());
            result.setTotal((long) voList.size()); // 更新为过滤后的实际数量
            result.setPages(taskPage.getPages());
            result.setRecords(voList);
            
            return ApiResult.success(result);
            
        } catch (Exception e) {
            return ApiResult.error("查询任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询可选择的任务（在数据库层面直接过滤状态）
     * 专门用于主界面的"选择生产订单"对话框
     * 只查询状态为"待生产"(0)、"生产中"(1)和"未启用但有采集数据"(3)的任务，在数据库层面直接过滤，不查询已完成状态
     * 
     * @param request 查询请求（包含分页参数和查询条件）
     * @return 分页结果
     */
    public ApiResult<PageOutput<TaskVO>> querySelectableTaskPageV2(TaskQueryRequest request) {
        try {
            // 1. 构建分页参数
            PageInput pageInput = new PageInput();
            pageInput.setCurrent(Long.valueOf(request.getPageNum()));
            pageInput.setSize(Long.valueOf(request.getPageSize()));
            
            // 2. 构建状态列表：查询待生产(0)、生产中(1)和未启用但有采集数据(3)状态的订单
            List<Integer> orderStatuses = java.util.Arrays.asList(0, 1, 3);
            
            // 3. 调用新的Repository方法，在数据库层面直接过滤状态
            PageOutput<Task> taskPage = taskRepository.findByPageWithStatuses(
                pageInput, 
                request.getOrderNo(),
                request.getSyBatchNo(),
                request.getProductName(),
                orderStatuses,
                request.getProductTimeStart(),
                request.getProductTimeEnd()
            );
            
            // 4. Domain Model → VO
            List<TaskVO> voList = taskPage.getRecords().stream()
                .map(TaskDtoMapper::toVO)
                .collect(Collectors.toList());
            
            // 5. 构建分页结果
            PageOutput<TaskVO> result = new PageOutput<>();
            result.setCurrent(taskPage.getCurrent());
            result.setSize(taskPage.getSize());
            result.setTotal(taskPage.getTotal());
            result.setPages(taskPage.getPages());
            result.setRecords(voList);
            
            return ApiResult.success(result);
            
        } catch (Exception e) {
            return ApiResult.error("查询任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新任务状态用例
     * @param id 任务ID
     * @param status 新状态
     * @param type 采集类型（可选）：1-有箱码，2-无箱码（启用任务时需要传递）
     * @param deviceConnectionsJson 设备连接状态JSON字符串（可选，启用任务时需要传递），格式：{"设备ID1":true,"设备ID2":false}
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Boolean> updateTaskStatus(Integer id, Integer status, Integer type, String deviceConnectionsJson) {
        try {
            // 如果是启用任务（status=1），需要校验采集模式切换
            if (status != null && status == 1) {
                ApiResult<Boolean> validationResult = validateModeSwitch(id, type);
                if (validationResult.getCode() != 200) {
                    return validationResult;
                }
                
                // 检查所有启用设备的连接状态
                ApiResult<Boolean> deviceCheckResult = checkDeviceConnections(deviceConnectionsJson);
                if (deviceCheckResult.getCode() != 200) {
                    return deviceCheckResult;
                }
            }
            
            // 如果是停用任务（status=0），需要检查CodeRelationUpload表中是否有未删除数据
            // 状态值说明：
            // 0: 待生产
            // 1: 生产中（已启用）
            // 2: 已完成
            // 3: 未启用但有采集数据（显示为"生产中"但需要点击启用任务）
            // 5: 提前结单
            Integer actualStatus = status;
            if (status != null && status == 0) {
                // 查询任务详情，获取OrderNo和ProductNo
                ProductionOrderDetailPO taskDetail = productionOrderDetailMapper.selectById(id);
                if (taskDetail != null) {
                    String orderNo = taskDetail.getOrderNo();
                    String productNo = taskDetail.getProductNo();
                    
                    // 查询CodeRelationUpload表中是否有该OrderNo和ProductNo的未删除数据
                    // 使用selectLatestByOrderAndProduct方法，只查询一条记录判断是否存在，性能优于COUNT
                    CodeRelationPO lastRecord = codeRelationMapper.selectLatestByOrderAndProduct(orderNo, productNo);
                    
                    // 如果有未删除的数据，将OrderStatus设为3（未启用但有采集数据）而不是0（停用）
                    // 状态3显示为"生产中"，但实际未启用，需要点击启用任务才能继续生产
                    if (lastRecord != null) {
                        System.out.println("[停用任务判断] 订单=" + orderNo + ", 产品=" + productNo + 
                                         ", CodeRelationUpload表中有未删除数据，将OrderStatus设为3（未启用但有采集数据）");
                        actualStatus = 3;
                    } else {
                        System.out.println("[停用任务判断] 订单=" + orderNo + ", 产品=" + productNo + 
                                         ", CodeRelationUpload表中无未删除数据，正常停用（OrderStatus=0）");
                    }
                }
            }
            
            boolean success = taskRepository.updateStatus(id, actualStatus);
            
            return success ? ApiResult.success(true)
                          : ApiResult.error("状态更新失败");
            
        } catch (Exception e) {
            return ApiResult.error("状态更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查所有启用设备的连接状态
     * @param deviceConnectionsJson 设备连接状态JSON字符串，格式：{"设备ID1":true,"设备ID2":false}
     * @return 操作结果，如果有未连接的设备则返回错误
     */
    private ApiResult<Boolean> checkDeviceConnections(String deviceConnectionsJson) {
        try {
            // 如果未传递设备连接状态信息，跳过检查（兼容旧版本）
            if (deviceConnectionsJson == null || deviceConnectionsJson.trim().isEmpty()) {
                System.out.println("[设备连接检查] 未传递设备连接状态信息，跳过检查");
                return ApiResult.success(true);
            }
            
            // 解析JSON字符串为Map
            Map<String, Boolean> deviceConnections;
            try {
                deviceConnections = objectMapper.readValue(
                    deviceConnectionsJson, 
                    new TypeReference<Map<String, Boolean>>() {}
                );
            } catch (Exception e) {
                System.err.println("[设备连接检查] 解析设备连接状态JSON失败: " + e.getMessage());
                // JSON解析失败时，跳过检查（兼容性考虑）
                return ApiResult.success(true);
            }
            
            // 获取所有设备
            List<IoDeviceDTO> allDevices = deviceApplicationService.getAllDevices();
            
            // 筛选出已启用的设备
            List<IoDeviceDTO> enabledDevices = allDevices.stream()
                .filter(device -> device.getEnabled() != null && device.getEnabled())
                .collect(Collectors.toList());
            
            // 检查每个启用设备的连接状态
            List<String> disconnectedDeviceNames = new ArrayList<>();
            for (IoDeviceDTO device : enabledDevices) {
                String deviceId = device.getId();
                Boolean isConnected = deviceConnections.get(deviceId);
                
                // 如果设备连接状态为null或false，则认为未连接
                if (isConnected == null || !isConnected) {
                    disconnectedDeviceNames.add(device.getDeviceName());
                }
            }
            
            // 如果有未连接的设备，返回错误
            if (!disconnectedDeviceNames.isEmpty()) {
                String errorMessage = "启用任务失败：以下设备未连接：" + String.join("、", disconnectedDeviceNames);
                System.out.println("[设备连接检查] " + errorMessage);
                return ApiResult.error(errorMessage);
            }
            
            System.out.println("[设备连接检查] 所有启用设备均已连接");
            return ApiResult.success(true);
            
        } catch (Exception e) {
            System.err.println("[设备连接检查] 检查设备连接状态异常: " + e.getMessage());
            e.printStackTrace();
            // 检查异常时，为了不影响正常启用，返回成功（可根据需求调整）
            return ApiResult.success(true);
        }
    }
    
    /**
     * 校验采集模式切换
     * 在启用任务时，检查是否可以切换采集模式（有箱码/无箱码）
     * @param taskId 任务ID
     * @param currentType 当前前端选择的采集类型（1=有箱码，2=无箱码）
     * @return 校验结果
     */
    private ApiResult<Boolean> validateModeSwitch(Integer taskId, Integer currentType) {
        try {
            // 1. 查询任务详情，获取订单号和产品编号
            ProductionOrderDetailPO taskDetail = productionOrderDetailMapper.selectById(taskId);
            if (taskDetail == null) {
                return ApiResult.error("任务不存在");
            }
            
            String orderNo = taskDetail.getOrderNo();
            String productNo = taskDetail.getProductNo();
            
            if (currentType == null) {
                // 如果Type为空，不进行校验
                System.out.println("[启用任务校验] 订单=" + orderNo + ", 产品=" + productNo + ", Type参数为空，不进行校验");
                return ApiResult.success(true);
            }
            
            // 2. 查询CodeRelationUpload表中当前生产订单和产品的最新插入的一条数据（未被删除）
            // 使用自定义SQL查询（TOP 1），确保只查询一条数据，性能最优
            // 重要：必须按OrderNo和ProductNo一起过滤，因为一个订单可能包含多个产品
            CodeRelationPO lastRecord = codeRelationMapper.selectLatestByOrderAndProduct(orderNo, productNo);
            List<CodeRelationPO> lastRecords = lastRecord != null ? java.util.Collections.singletonList(lastRecord) : java.util.Collections.emptyList();
            
            System.out.println("[启用任务校验-详细] 订单=" + orderNo + 
                             ", 产品=" + productNo +
                             ", 查询到记录数=" + (lastRecords != null ? lastRecords.size() : 0) +
                             ", 当前Type=" + currentType);
            
            // 如果没有历史数据，允许启用（首次启用）
            if (lastRecord == null) {
                System.out.println("[启用任务校验] 订单=" + orderNo + ", 产品=" + productNo + ", 无历史数据，允许启用");
                return ApiResult.success(true);
            }
            
            Integer lastType = lastRecord.getType(); // 上次的Type
            String bigSerialNumber = lastRecord.getBigSerialNumber();
            
            System.out.println("[启用任务校验-详细] 最新记录: " + 
                             "ID=" + lastRecord.getId() +
                             ", Type=" + lastType + 
                             ", BigSerialNumber=" + (bigSerialNumber == null ? "null" : "'" + bigSerialNumber + "'") +
                             ", SmallSerialNumber=" + lastRecord.getSmallSerialNumber() +
                             ", AddTime=" + lastRecord.getAddTime());
            
            // 3. 判断Type是否一致
            if (lastType != null && !lastType.equals(currentType)) {
                // Type不一致，需要检查BigSerialNumber字段
                
                // 判断BigSerialNumber是否有值
                if (bigSerialNumber == null || bigSerialNumber.trim().isEmpty()) {
                    // BigSerialNumber为空，说明有未进行关联的码
                    String lastModeText = (lastType == 1) ? "有箱码" : "无箱码";
                    String currentModeText = (currentType == 1) ? "有箱码" : "无箱码";
                    
                    String errorMsg = "上次采集模式(" + lastModeText + ")仍有未进行关联的码，无法切换到" + currentModeText + "模式。" +
                                    "请先完成托盘码关联后再切换采集模式。";
                    
                    System.err.println("[启用任务校验-禁止] 订单=" + orderNo + 
                                     ", 产品=" + productNo +
                                     ", 上次Type=" + lastType + 
                                     ", 当前Type=" + currentType + 
                                     ", BigSerialNumber为空，禁止切换");
                    
                    return ApiResult.error(errorMsg);
                }
                
                // BigSerialNumber有值，允许切换
                System.out.println("[启用任务校验-允许] 订单=" + orderNo + 
                                 ", 产品=" + productNo +
                                 ", 上次Type=" + lastType + 
                                 ", 当前Type=" + currentType + 
                                 ", BigSerialNumber有值('" + bigSerialNumber + "')，允许切换");
            } else {
                // Type一致，允许启用
                System.out.println("[启用任务校验-允许] 订单=" + orderNo + 
                                 ", 产品=" + productNo +
                                 ", Type一致(上次=" + lastType + ", 当前=" + currentType + ")，允许启用");
            }
            
            return ApiResult.success(true);
            
        } catch (Exception e) {
            System.err.println("[启用任务校验] 校验异常: " + e.getMessage());
            e.printStackTrace();
            // 校验异常时，为了不影响正常启用，返回成功
            return ApiResult.success(true);
        }
    }
    
    /**
     * 查询所有任务用例
     * @return 任务列表
     */
    public ApiResult<List<TaskVO>> queryAllTasks() {
        try {
            List<Task> tasks = taskRepository.findAll();
            
            List<TaskVO> voList = tasks.stream()
                .map(TaskDtoMapper::toVO)
                .collect(Collectors.toList());
            
            return ApiResult.success(voList);
            
        } catch (Exception e) {
            return ApiResult.error("查询任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询生产订单完成度
     * 根据OrderNo和ProductNo查询ProductionOrderDetail表，计算OrderCount/ProductCount
     * 用于主界面单位实时统计的完成度显示
     * 
     * @param orderNo 订单编号
     * @param productNo 产品编号
     * @return 完成度（百分比，0-100）
     */
    public ApiResult<Double> getCompletionRate(String orderNo, String productNo) {
        try {
            if (orderNo == null || orderNo.trim().isEmpty()) {
                return ApiResult.error("订单编号不能为空");
            }
            if (productNo == null || productNo.trim().isEmpty()) {
                return ApiResult.error("产品编号不能为空");
            }
            
            // 查询ProductionOrderDetail表中对应的OrderNo和ProductNo的记录
            List<ProductionOrderDetailPO> orderDetails = productionOrderDetailMapper.selectList(
                new LambdaQueryWrapper<ProductionOrderDetailPO>()
                    .eq(ProductionOrderDetailPO::getOrderNo, orderNo)
                    .eq(ProductionOrderDetailPO::getProductNo, productNo)
                    .eq(ProductionOrderDetailPO::getIsDel, 0)
            );
            
            if (orderDetails == null || orderDetails.isEmpty()) {
                return ApiResult.error("未找到对应的生产订单明细");
            }
            
            // 取第一条记录（正常情况下应该只有一条）
            ProductionOrderDetailPO orderDetail = orderDetails.get(0);
            
            Integer orderCount = orderDetail.getOrderCount(); // 已生产数量
            Integer productCount = orderDetail.getProductCount(); // 计划订单数量
            
            if (orderCount == null) {
                orderCount = 0;
            }
            if (productCount == null || productCount == 0) {
                return ApiResult.error("计划订单数量为0，无法计算完成度");
            }
            
            // 计算完成度：OrderCount/ProductCount * 100
            double completionRate = (double) orderCount / productCount * 100;
            
            System.out.println("[完成度查询] 订单=" + orderNo + ", 产品=" + productNo + 
                             ", OrderCount=" + orderCount + ", ProductCount=" + productCount + 
                             ", 完成度=" + String.format("%.1f%%", completionRate));
            
            return ApiResult.success("查询成功", completionRate);
            
        } catch (Exception e) {
            System.err.println("[完成度查询] 失败：订单=" + orderNo + ", 产品=" + productNo + ", 错误=" + e.getMessage());
            e.printStackTrace();
            return ApiResult.error("查询完成度失败: " + e.getMessage());
        }
    }
}


