package com.miduo.cloud.entity.dto.code;

/**
 * 托盘码关联结果
 */
public class PalletAssociateResult {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 关联的标签编号
     */
    private String tagNo;
    
    /**
     * 托盘码
     */
    private String palletCode;
    
    /**
     * 虚拟垛标
     */
    private String virtualPalletCode;
    
    /**
     * 更新的记录数
     */
    private Integer updatedCount;
    
    public PalletAssociateResult() {
    }
    
    public static PalletAssociateResult success(String tagNo, String palletCode, 
                                                 String virtualPalletCode, Integer updatedCount) {
        PalletAssociateResult result = new PalletAssociateResult();
        result.setSuccess(true);
        result.setMessage("托盘码关联成功");
        result.setTagNo(tagNo);
        result.setPalletCode(palletCode);
        result.setVirtualPalletCode(virtualPalletCode);
        result.setUpdatedCount(updatedCount);
        return result;
    }
    
    public static PalletAssociateResult error(String message) {
        PalletAssociateResult result = new PalletAssociateResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
    
    // Getters and Setters
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getTagNo() {
        return tagNo;
    }
    
    public void setTagNo(String tagNo) {
        this.tagNo = tagNo;
    }
    
    public String getPalletCode() {
        return palletCode;
    }
    
    public void setPalletCode(String palletCode) {
        this.palletCode = palletCode;
    }
    
    public String getVirtualPalletCode() {
        return virtualPalletCode;
    }
    
    public void setVirtualPalletCode(String virtualPalletCode) {
        this.virtualPalletCode = virtualPalletCode;
    }
    
    public Integer getUpdatedCount() {
        return updatedCount;
    }
    
    public void setUpdatedCount(Integer updatedCount) {
        this.updatedCount = updatedCount;
    }
    
    @Override
    public String toString() {
        return "PalletAssociateResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", tagNo='" + tagNo + '\'' +
                ", palletCode='" + palletCode + '\'' +
                ", virtualPalletCode='" + virtualPalletCode + '\'' +
                ", updatedCount=" + updatedCount +
                '}';
    }
}

