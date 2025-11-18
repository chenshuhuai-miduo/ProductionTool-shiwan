package com.miduo.cloud.entity.dto.code;

/**
 * 调整箱数请求
 */
public class AdjustBoxCountRequest {
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 产品编号
     */
    private String productNo;
    
    /**
     * 目标箱数
     */
    private Integer targetCount;
    
    /**
     * 每垛箱数（来自采集规格）
     */
    private Integer boxesPerPallet;
    
    public AdjustBoxCountRequest() {
    }
    
    public AdjustBoxCountRequest(String orderNo, Integer targetCount) {
        this.orderNo = orderNo;
        this.targetCount = targetCount;
    }
    
    public AdjustBoxCountRequest(String orderNo, Integer targetCount, Integer boxesPerPallet) {
        this.orderNo = orderNo;
        this.targetCount = targetCount;
        this.boxesPerPallet = boxesPerPallet;
    }
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    
    public Integer getTargetCount() {
        return targetCount;
    }
    
    public void setTargetCount(Integer targetCount) {
        this.targetCount = targetCount;
    }
    
    public Integer getBoxesPerPallet() {
        return boxesPerPallet;
    }
    
    public void setBoxesPerPallet(Integer boxesPerPallet) {
        this.boxesPerPallet = boxesPerPallet;
    }
    
    public String getProductNo() {
        return productNo;
    }
    
    public void setProductNo(String productNo) {
        this.productNo = productNo;
    }
}
