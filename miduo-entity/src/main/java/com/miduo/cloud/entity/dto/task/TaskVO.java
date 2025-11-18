package com.miduo.cloud.entity.dto.task;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务视图对象VO
 */
@Data
public class TaskVO {
    
    /**
     * 任务详情ID（ProductionOrderDetail表的主键）
     */
    private Integer id;
    
    /**
     * 订单ID
     */
    private Integer orderId;
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 产品编号
     */
    private String productNo;
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 产品规格
     */
    private String productFormatName;
    
    /**
     * 计划数量
     */
    private Integer productCount;
    
    /**
     * 完成数量
     */
    private Integer orderCount;
    
    /**
     * 采集规格（拖箱比例）
     */
    private Integer ratio;
    
    /**
     * 生产批次
     */
    private String syBatchNo;
    
    /**
     * 生产日期
     */
    private LocalDateTime productTime;
    
    /**
     * 预计完工时间
     */
    private LocalDateTime twillendTime;
    
    /**
     * 下单日期
     */
    private LocalDateTime createTime;
    
    /**
     * 订单状态 (0: 待入库, 1: 入库中, 2: 入库完成, 5: 提前结单)
     */
    private Integer orderStatus;
    
    /**
     * 订单状态文字
     */
    private String orderStatusText;
    
    /**
     * 类型 (1: 有箱码, 2: 无箱码)
     */
    private Integer type;
    
    /**
     * 类型文字
     */
    private String typeText;
    
    /**
     * 备注
     */
    private String remark;
}

