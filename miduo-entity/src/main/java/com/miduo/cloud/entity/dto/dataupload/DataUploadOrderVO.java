package com.miduo.cloud.entity.dto.dataupload;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据上传页面-生产订单VO
 */
@Data
public class DataUploadOrderVO {
    
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
     * 计划订单数量
     */
    private Integer productCount;
    
    /**
     * 完成数量（统计CodeRelationUpload表中BigSerialNumber有值的数量）
     */
    private Integer completedCount;
    
    /**
     * 生产日期
     */
    private LocalDateTime productTime;
    
    /**
     * 类型 1有箱码 2无箱码
     */
    private Integer type;
    
    /**
     * 生产状态（OrderStatus）
     */
    private Integer productionStatus;
    
    /**
     * 上传状态（默认为上传完成）
     */
    private String uploadStatus;
}

