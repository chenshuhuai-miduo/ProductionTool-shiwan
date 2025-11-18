package com.miduo.cloud.entity.dto.code;

/**
 * 当前垛信息VO
 */
public class CurrentPalletInfoVO {
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 当前垛的标签编号
     */
    private String tagNo;
    
    /**
     * 当前垛已采集数量
     */
    private Integer currentCount;
    
    /**
     * 每垛箱数
     */
    private Integer totalCount;
    
    /**
     * 是否满垛
     */
    private Boolean isPalletFull;
    
    /**
     * 完成进度百分比
     */
    private Double progressPercentage;
    
    public CurrentPalletInfoVO() {
    }
    
    public static CurrentPalletInfoVO create(String orderNo, String tagNo, 
                                             Integer currentCount, Integer totalCount) {
        CurrentPalletInfoVO vo = new CurrentPalletInfoVO();
        vo.setOrderNo(orderNo);
        vo.setTagNo(tagNo);
        vo.setCurrentCount(currentCount);
        vo.setTotalCount(totalCount);
        vo.setIsPalletFull(currentCount >= totalCount);
        
        if (totalCount > 0) {
            vo.setProgressPercentage((double) currentCount / totalCount * 100);
        } else {
            vo.setProgressPercentage(0.0);
        }
        
        return vo;
    }
    
    // Getters and Setters
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    
    public String getTagNo() {
        return tagNo;
    }
    
    public void setTagNo(String tagNo) {
        this.tagNo = tagNo;
    }
    
    public Integer getCurrentCount() {
        return currentCount;
    }
    
    public void setCurrentCount(Integer currentCount) {
        this.currentCount = currentCount;
    }
    
    public Integer getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
    
    public Boolean getIsPalletFull() {
        return isPalletFull;
    }
    
    public void setIsPalletFull(Boolean isPalletFull) {
        this.isPalletFull = isPalletFull;
    }
    
    public Double getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    @Override
    public String toString() {
        return "CurrentPalletInfoVO{" +
                "orderNo='" + orderNo + '\'' +
                ", tagNo='" + tagNo + '\'' +
                ", currentCount=" + currentCount +
                ", totalCount=" + totalCount +
                ", isPalletFull=" + isPalletFull +
                ", progressPercentage=" + progressPercentage +
                '}';
    }
}

