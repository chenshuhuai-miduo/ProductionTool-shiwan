package com.miduo.cloud.domain.task.repository;

import com.miduo.cloud.domain.task.model.Task;
import com.miduo.cloud.common.dto.PageInput;
import com.miduo.cloud.common.dto.PageOutput;

import java.util.List;
import java.util.Optional;

/**
 * 任务仓储接口（Repository）
 * 定义任务数据访问的抽象接口，具体实现在 infrastructure 层
 */
public interface TaskRepository {
    
    /**
     * 保存任务
     * @param task 任务领域对象
     * @return 生成的订单号
     */
    String save(Task task);
    
    /**
     * 根据ID查询任务
     * @param id 任务ID
     * @return 任务领域对象
     */
    Optional<Task> findById(Integer id);
    
    /**
     * 根据订单号查询任务
     * @param orderNo 订单号
     * @return 任务领域对象
     */
    Optional<Task> findByOrderNo(String orderNo);
    
    /**
     * 分页查询任务列表
     * @param pageInput 分页参数
     * @param orderNo 订单号（可选，模糊查询）
     * @param type 类型（可选）
     * @return 分页结果
     */
    PageOutput<Task> findByPage(PageInput pageInput, String orderNo, Integer type);
    
    /**
     * 分页查询任务列表（支持更多筛选条件）
     * @param pageInput 分页参数
     * @param orderNo 订单号（可选，模糊查询）
     * @param syBatchNo 生产批次（可选，模糊查询）
     * @param productName 产品名称（可选，模糊查询）
     * @param orderStatus 订单状态（可选，精确匹配）
     * @param productTimeStart 预计生产时间范围 - 开始时间（可选）
     * @param productTimeEnd 预计生产时间范围 - 结束时间（可选）
     * @return 分页结果
     */
    PageOutput<Task> findByPageWithFilters(PageInput pageInput, String orderNo, 
                                           String syBatchNo, String productName, Integer orderStatus,
                                           java.time.LocalDateTime productTimeStart, java.time.LocalDateTime productTimeEnd);
    
    /**
     * 分页查询任务列表（支持多个订单状态筛选，在数据库层面直接过滤）
     * 用于选择生产订单页面，只查询待生产(0)和生产中(1)状态的订单
     * @param pageInput 分页参数
     * @param orderNo 订单号（可选，模糊查询）
     * @param syBatchNo 生产批次（可选，模糊查询）
     * @param productName 产品名称（可选，模糊查询）
     * @param orderStatuses 订单状态列表（支持多个状态，如：0待生产，1生产中）
     * @param productTimeStart 预计生产时间范围 - 开始时间（可选）
     * @param productTimeEnd 预计生产时间范围 - 结束时间（可选）
     * @return 分页结果
     */
    PageOutput<Task> findByPageWithStatuses(PageInput pageInput, String orderNo,
                                            String syBatchNo, String productName, List<Integer> orderStatuses,
                                            java.time.LocalDateTime productTimeStart, java.time.LocalDateTime productTimeEnd);
    
    /**
     * 更新任务
     * @param task 任务领域对象
     * @return 是否更新成功
     */
    boolean update(Task task);
    
    /**
     * 删除任务（逻辑删除）
     * @param id 任务ID
     * @return 是否删除成功
     */
    boolean deleteById(Integer id);
    
    /**
     * 更新任务状态
     * @param id 任务ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateStatus(Integer id, Integer status);
    
    /**
     * 查询所有未删除的任务
     * @return 任务列表
     */
    List<Task> findAll();
}

