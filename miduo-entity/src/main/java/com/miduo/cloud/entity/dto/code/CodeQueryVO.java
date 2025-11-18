package com.miduo.cloud.entity.dto.code;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 码查询结果DTO
 * 包含码信息、产品信息、关联码信息
 */
@Data
public class CodeQueryVO {
    
    // ==================== 基础码信息 ====================
    /**
     * 箱码（查询的码）
     */
    private String smallSerialNumber;
    
    /**
     * 托盘码
     */
    private String bigSerialNumber;
    
    /**
     * 更大标
     */
    private String biggerSerialNumber;
    
    /**
     * 中标
     */
    private String mediumSerialNumber;
    
    /**
     * 虚拟标
     */
    private String virtualSerialNumber;
    
    /**
     * 是否关联（1为已关联，0为未关联）
     */
    private Integer isVirtual;
    
    /**
     * DX码
     */
    private String dxCode;
    
    /**
     * 销售码
     */
    private String salesCode;
    
    /**
     * 托标号
     */
    private String tagNo;
    
    /**
     * 采集时间
     */
    private LocalDateTime addTime;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    
    // ==================== 产品和订单信息 ====================
    /**
     * 产品编号
     */
    private String productNo;
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 批次号
     */
    private String batchNo;
    
    /**
     * 类型（1-有箱码，2-无箱码）
     */
    private Integer type;
    
    /**
     * 状态
     */
    private Integer status;
    
    // ==================== 生产订单详情信息 ====================
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 产品规格名称
     */
    private String productFormatName;
    
    /**
     * 生产批次
     */
    private String syBatchNo;
    
    /**
     * 比率
     */
    private Integer ratio;
    
    /**
     * 生产时间
     */
    private LocalDateTime productTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime twillendTime;
}

