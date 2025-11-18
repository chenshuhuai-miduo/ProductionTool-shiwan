package com.miduo.cloud.entity.dto.code;

/**
 * 托盘码关联请求
 * 第三、四台设备：根据第三台的触发箱码，将托盘码关联到该箱码所在垛的所有箱码
 */
public class PalletAssociateRequest {
    
    /**
     * 托盘码（BigSerialNumber）
     */
    private String palletCode;
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 产品编号
     */
    private String productNo;
    
    /**
     * 触发箱码（第三台设备扫描的箱码，用于查找TagNo）
     */
    private String triggerBoxCode;
    
    /**
     * 每垛箱数（Qty）
     * 批量更新时，只更新最新的Qty个记录
     */
    private Integer qty;
    
    public PalletAssociateRequest() {
    }
    
    public PalletAssociateRequest(String palletCode, String orderNo) {
        this.palletCode = palletCode;
        this.orderNo = orderNo;
    }
    
    public String getPalletCode() {
        return palletCode;
    }
    
    public void setPalletCode(String palletCode) {
        this.palletCode = palletCode;
    }
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    
    public String getTriggerBoxCode() {
        return triggerBoxCode;
    }
    
    public void setTriggerBoxCode(String triggerBoxCode) {
        this.triggerBoxCode = triggerBoxCode;
    }
    
    public Integer getQty() {
        return qty;
    }
    
    public void setQty(Integer qty) {
        this.qty = qty;
    }
    
    public String getProductNo() {
        return productNo;
    }
    
    public void setProductNo(String productNo) {
        this.productNo = productNo;
    }
    
    @Override
    public String toString() {
        return "PalletAssociateRequest{" +
                "palletCode='" + palletCode + '\'' +
                ", orderNo='" + orderNo + '\'' +
                ", productNo='" + productNo + '\'' +
                ", triggerBoxCode='" + triggerBoxCode + '\'' +
                ", qty=" + qty +
                '}';
    }
}

