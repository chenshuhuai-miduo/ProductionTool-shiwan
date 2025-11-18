package com.miduo.cloud.entity.dto.code;

import java.util.List;

/**
 * 强制满垛结果
 */
public class ForcePalletResult {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * TagNo
     */
    private String tagNo;
    
    /**
     * 原有箱数
     */
    private Integer originalCount;
    
    /**
     * 每垛箱数（目标数）
     */
    private Integer boxesPerPallet;
    
    /**
     * 补充的虚拟箱码数量
     */
    private Integer virtualBoxesAdded;
    
    /**
     * 最终箱数
     */
    private Integer finalCount;
    
    /**
     * 生成的虚拟箱码列表
     */
    private List<String> generatedCodes;
    
    public static ForcePalletResult success(String tagNo, Integer originalCount, Integer boxesPerPallet, 
                                           Integer virtualBoxesAdded, String message, List<String> generatedCodes) {
        ForcePalletResult result = new ForcePalletResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setTagNo(tagNo);
        result.setOriginalCount(originalCount);
        result.setBoxesPerPallet(boxesPerPallet);
        result.setVirtualBoxesAdded(virtualBoxesAdded);
        result.setFinalCount(boxesPerPallet); // 强制满垛后，最终箱数就是每垛箱数
        result.setGeneratedCodes(generatedCodes);
        return result;
    }
    
    public static ForcePalletResult error(String message) {
        ForcePalletResult result = new ForcePalletResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
    
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
    
    public Integer getOriginalCount() {
        return originalCount;
    }
    
    public void setOriginalCount(Integer originalCount) {
        this.originalCount = originalCount;
    }
    
    public Integer getBoxesPerPallet() {
        return boxesPerPallet;
    }
    
    public void setBoxesPerPallet(Integer boxesPerPallet) {
        this.boxesPerPallet = boxesPerPallet;
    }
    
    public Integer getVirtualBoxesAdded() {
        return virtualBoxesAdded;
    }
    
    public void setVirtualBoxesAdded(Integer virtualBoxesAdded) {
        this.virtualBoxesAdded = virtualBoxesAdded;
    }
    
    public Integer getFinalCount() {
        return finalCount;
    }
    
    public void setFinalCount(Integer finalCount) {
        this.finalCount = finalCount;
    }
    
    public List<String> getGeneratedCodes() {
        return generatedCodes;
    }
    
    public void setGeneratedCodes(List<String> generatedCodes) {
        this.generatedCodes = generatedCodes;
    }
    
    @Override
    public String toString() {
        return "ForcePalletResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", tagNo='" + tagNo + '\'' +
                ", originalCount=" + originalCount +
                ", boxesPerPallet=" + boxesPerPallet +
                ", virtualBoxesAdded=" + virtualBoxesAdded +
                ", finalCount=" + finalCount +
                ", generatedCodes=" + generatedCodes +
                '}';
    }
}

