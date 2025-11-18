package com.miduo.cloud.entity.dto.code;

/**
 * 生成系统箱码请求（对应addCode接口）
 */
public class GenerateSystemCodeRequest {
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 产品编号
     */
    private String productNo;
    
    /**
     * 生产批次
     */
    private String batchNo;
    
    /**
     * 类型：1=有箱码，2=无箱码
     */
    private Integer type;
    
    /**
     * 每垛箱数
     */
    private Integer boxCount;
    
    /**
     * 垛号（可选）
     */
    private Integer stackCount;
    
    public GenerateSystemCodeRequest() {
    }
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    
    public String getProductNo() {
        return productNo;
    }
    
    public void setProductNo(String productNo) {
        this.productNo = productNo;
    }
    
    public String getBatchNo() {
        return batchNo;
    }
    
    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }
    
    public Integer getType() {
        return type;
    }
    
    public void setType(Integer type) {
        this.type = type;
    }
    
    public Integer getBoxCount() {
        return boxCount;
    }
    
    public void setBoxCount(Integer boxCount) {
        this.boxCount = boxCount;
    }
    
    public Integer getStackCount() {
        return stackCount;
    }
    
    public void setStackCount(Integer stackCount) {
        this.stackCount = stackCount;
    }
}
