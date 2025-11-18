package com.miduo.cloud.entity.dto.task;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务修改请求DTO
 */
@Data
public class TaskUpdateRequest {
    
    /**
     * 任务ID（用于根据ID修改）
     */
    private Integer id;
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 产品ID
     */
    private Integer productId;
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 产品编号
     */
    private String productNo;
    
    /**
     * 规格ID
     */
    private Integer productFormatId;
    
    /**
     * 规格名称
     */
    private String productFormatName;
    
    /**
     * 计划订单数量
     */
    private Integer productCount;
    
    /**
     * 批次信息
     */
    private String syBatchNo;
    
    /**
     * 类型 (1: 有箱码, 2: 无箱码)
     */
    private Integer type;
    
    /**
     * 拖箱比例
     */
    private Integer ratio;
    
    /**
     * 生产时间
     */
    private LocalDateTime productTime;
    
    /**
     * 预计完工时间
     */
    private LocalDateTime twillendTime;
}

