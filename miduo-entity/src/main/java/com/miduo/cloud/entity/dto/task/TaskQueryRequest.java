package com.miduo.cloud.entity.dto.task;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务查询请求DTO
 */
@Data
public class TaskQueryRequest {
    
    /**
     * 当前页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页条数
     */
    private Integer pageSize = 20;
    
    /**
     * 生产订单（模糊查询）
     */
    private String orderNo;
    
    /**
     * 生产批次（模糊查询）
     */
    private String syBatchNo;
    
    /**
     * 产品名称（模糊查询）
     */
    private String productName;
    
    /**
     * 订单状态筛选
     */
    private Integer orderStatus;
    
    /**
     * 预计生产时间范围 - 开始时间
     */
    private LocalDateTime productTimeStart;
    
    /**
     * 预计生产时间范围 - 结束时间
     */
    private LocalDateTime productTimeEnd;
}

