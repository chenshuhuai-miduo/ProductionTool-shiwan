package com.miduo.cloud.domain.code.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 码关联领域模型
 * 代表产线上的一条码关联记录（箱码、托盘码等）
 */
@Data
public class CodeRelation {
    
    // ===== 基本信息 =====
    private Integer id;
    private String orderNo;              // 订单号
    private String productNo;            // 产品编号
    
    // ===== 码信息 =====
    private String biggerSerialNumber;   // 大码
    private String bigSerialNumber;      // 大码
    private String mediumSerialNumber;   // 中码
    private String smallSerialNumber;    // 小码（箱码）
    private String dxCode;               // DX码
    private String salesCode;            // 销售码
    private String virtualSerialNumber;  // 虚拟序列号
    
    // ===== 关联信息 =====
    private String tagNo;                // 标签号（托盘标识）
    private Integer qty;                 // 每托盘箱数
    private Integer isVirtual;           // 是否虚拟码
    
    // ===== 状态信息 =====
    private Integer status;              // 状态
    private Integer type;                // 类型
    private String warehouseNo;          // 仓库号
    private String batchNo;              // 批次号
    private Integer isDel;               // 是否删除
    
    // ===== 上传信息 =====
    private Integer isUpload;            // 是否已上传
    private LocalDateTime uploadTime;    // 上传时间
    private Integer errCount;            // 错误次数
    private String msg;                  // 消息
    
    // ===== 系统信息 =====
    private LocalDateTime addTime;       // 添加时间
    private String teamName;             // 班组名称
    
    // ===== 业务方法 =====
    
    /**
     * 判断是否为虚拟码
     */
    public boolean isVirtualCode() {
        return Integer.valueOf(1).equals(this.isVirtual);
    }
    
    /**
     * 判断箱码是否有效（仅检查非空）
     */
    public boolean isValidBoxCode() {
        return this.smallSerialNumber != null 
            && !this.smallSerialNumber.isEmpty();
    }
    
    /**
     * 判断是否已关联托盘
     */
    public boolean hasPallet() {
        return this.tagNo != null && !this.tagNo.isEmpty();
    }
    
    /**
     * 关联托盘码
     */
    public void associatePallet(String tagNo, String palletCode, Integer qty) {
        this.tagNo = tagNo;
        this.biggerSerialNumber = palletCode;
        this.qty = qty;
    }
    
    /**
     * 标记为虚拟码
     */
    public void markAsVirtual() {
        this.isVirtual = 1;
        this.virtualSerialNumber = this.smallSerialNumber;
    }
    
    /**
     * 判断是否为系统生成的箱码（22位）
     */
    public boolean isSystemGeneratedCode() {
        return this.smallSerialNumber != null 
            && this.smallSerialNumber.length() == 22;
    }
    
    /**
     * 逻辑删除
     */
    public void delete() {
        this.isDel = 1;
    }
    
    /**
     * 验证码关联数据完整性
     */
    public boolean validate() {
        return this.orderNo != null && !this.orderNo.isEmpty()
            && this.smallSerialNumber != null && !this.smallSerialNumber.isEmpty();
    }
}

