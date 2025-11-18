package com.miduo.cloud.entity.dto.code;

/**
 * 强制满垛请求
 */
public class ForcePalletRequest {
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 产品编号
     */
    private String productNo;
    
    /**
     * 每垛箱数（从前端采集规格传入）
     */
    private Integer boxesPerPallet;
    
    public ForcePalletRequest() {
    }
    
    public ForcePalletRequest(String orderNo) {
        this.orderNo = orderNo;
    }
    
    public ForcePalletRequest(String orderNo, Integer boxesPerPallet) {
        this.orderNo = orderNo;
        this.boxesPerPallet = boxesPerPallet;
    }
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
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
