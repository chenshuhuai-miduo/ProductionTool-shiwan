package com.miduo.cloud.entity.dto.task;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务添加请求DTO
 */
@Data
public class TaskAddRequest {
    
    /**
     * 订单编号（自动生成）
     */
    private String orderNo;
    
    /**
     * 类型 (1: 有箱码, 2: 无箱码)
     */
    private Integer type;
    
    /**
     * 生产总数量
     */
    private Integer orderSumCount;
    
    /**
     * 订单总数量
     */
    private Integer productSumCount;
    
    /**
     * 制单日期
     */
    private LocalDateTime dmakeDate;
    
    // ===== ProductionOrderDetail 字段 =====
    
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

