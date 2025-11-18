package com.miduo.cloud.entity.dto.code;

import lombok.Data;

/**
 * 无箱码采集请求
 * 
 * @author System
 * @since 2025-10-30
 */
@Data
public class NoBoxCollectRequest {
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 产品编号
     */
    private String productNo;
    
    /**
     * 生产批次号
     */
    private String batchNo;
    
    /**
     * 托盘码（第四台设备扫描）
     */
    private String palletCode;
    
    /**
     * 每垛箱数
     */
    private Integer boxesPerPallet;
}

