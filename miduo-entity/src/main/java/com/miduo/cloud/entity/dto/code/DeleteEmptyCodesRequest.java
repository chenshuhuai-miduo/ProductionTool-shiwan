package com.miduo.cloud.entity.dto.code;

/**
 * 删除无码请求
 */
public class DeleteEmptyCodesRequest {
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 产品编号
     */
    private String productNo;
    
    public DeleteEmptyCodesRequest() {
    }
    
    public DeleteEmptyCodesRequest(String orderNo) {
        this.orderNo = orderNo;
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
}
