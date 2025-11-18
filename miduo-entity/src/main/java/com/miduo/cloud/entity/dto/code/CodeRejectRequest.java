package com.miduo.cloud.entity.dto.code;

/**
 * 读码剔除校验请求
 */
public class CodeRejectRequest {
    
    /**
     * 设备序号
     */
    private Integer deviceOrder;
    
    /**
     * 码值（可能为null代表无码）
     */
    private String code;
    
    /**
     * 订单编号（用于校验重码）
     */
    private String orderNo;
    
    public CodeRejectRequest() {
    }
    
    public CodeRejectRequest(Integer deviceOrder, String code, String orderNo) {
        this.deviceOrder = deviceOrder;
        this.code = code;
        this.orderNo = orderNo;
    }
    
    public Integer getDeviceOrder() {
        return deviceOrder;
    }
    
    public void setDeviceOrder(Integer deviceOrder) {
        this.deviceOrder = deviceOrder;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
}

