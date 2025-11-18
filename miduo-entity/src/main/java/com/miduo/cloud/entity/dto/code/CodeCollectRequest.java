package com.miduo.cloud.entity.dto.code;

/**
 * 箱码采集请求
 * 第二台设备：采集箱码，存入数据库
 */
public class CodeCollectRequest {
    
    /**
     * 箱码（VirtualSerialNumber）
     */
    private String boxCode;
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 产品编号
     */
    private String productNo;
    
    /**
     * 批次号
     */
    private String batchNo;
    
    /**
     * 类型（1:有箱码 2:无箱码）
     */
    private Integer type;
    
    /**
     * 每垛箱数（Qty）
     */
    private Integer boxesPerPallet;
    
    /**
     * 班组名称（可选）
     */
    private String teamName;
    
    public CodeCollectRequest() {
    }
    
    public String getBoxCode() {
        return boxCode;
    }
    
    public void setBoxCode(String boxCode) {
        this.boxCode = boxCode;
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
    
    public Integer getBoxesPerPallet() {
        return boxesPerPallet;
    }
    
    public void setBoxesPerPallet(Integer boxesPerPallet) {
        this.boxesPerPallet = boxesPerPallet;
    }
    
    public String getTeamName() {
        return teamName;
    }
    
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    
    @Override
    public String toString() {
        return "CodeCollectRequest{" +
                "boxCode='" + boxCode + '\'' +
                ", orderNo='" + orderNo + '\'' +
                ", productNo='" + productNo + '\'' +
                ", batchNo='" + batchNo + '\'' +
                ", type=" + type +
                ", boxesPerPallet=" + boxesPerPallet +
                ", teamName='" + teamName + '\'' +
                '}';
    }
}

