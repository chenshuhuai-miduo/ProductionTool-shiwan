package com.miduo.cloud.domain.task.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务领域模型（Domain Model）
 * 代表生产线上的一个生产任务
 */
@Data
public class Task {
    
    // ===== 基本信息 =====
    private Integer id;
    private String orderNo;              // 订单编号
    private Integer type;                // 类型 (1: 有箱码, 2: 无箱码)
    private Integer orderStatus;         // 订单状态
    
    // ===== 数量信息 =====
    private Integer orderSumCount;       // 生产总数量
    private Integer productSumCount;     // 订单总数量
    private Integer orderCount;          // 已生产数量
    private Integer productCount;        // 计划订单数量
    
    // ===== 产品信息 =====
    private Integer productId;
    private String productName;
    private String productNo;
    private Integer productFormatId;
    private String productFormatName;
    
    // ===== 生产信息 =====
    private String syBatchNo;            // 批次信息
    private Integer ratio;               // 拖箱比例
    private LocalDateTime dmakeDate;     // 制单日期
    private LocalDateTime productTime;   // 生产时间
    private LocalDateTime twillendTime;  // 预计完工时间
    
    // ===== 系统信息 =====
    private LocalDateTime createTime;
    private Integer isDel;
    
    // ===== 业务方法 =====
    
    /**
     * 判断是否为有箱码类型
     */
    public boolean hasBoxCode() {
        return Integer.valueOf(1).equals(this.type);
    }
    
    /**
     * 判断是否为无箱码类型
     */
    public boolean noBoxCode() {
        return Integer.valueOf(2).equals(this.type);
    }
    
    /**
     * 判断任务是否已完成
     */
    public boolean isCompleted() {
        return this.orderCount != null && this.productCount != null 
            && this.orderCount.equals(this.productCount);
    }
    
    /**
     * 获取完成进度百分比
     */
    public Double getProgress() {
        if (this.productCount == null || this.productCount == 0) {
            return 0.0;
        }
        if (this.orderCount == null) {
            return 0.0;
        }
        return (this.orderCount * 100.0) / this.productCount;
    }
    
    /**
     * 更新生产数量
     */
    public void updateOrderCount(Integer count) {
        this.orderCount = count;
    }
    
    /**
     * 验证任务数据完整性
     */
    public boolean validate() {
        return this.productNo != null && !this.productNo.isEmpty()
            && this.productCount != null && this.productCount > 0;
    }
}

