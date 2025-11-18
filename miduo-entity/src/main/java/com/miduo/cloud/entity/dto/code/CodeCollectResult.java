package com.miduo.cloud.entity.dto.code;

/**
 * 箱码采集结果
 */
public class CodeCollectResult {
    
    /**
     * 采集状态：SUCCESS-成功, NO_CODE-无码, DUPLICATE-重复码, INVALID-错码
     */
    private String status;
    
    /**
     * 消息
     */
    private String message;
    
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
     * 生成的箱码（用于无码情况）
     */
    private String generatedBoxCode;
    
    public CodeCollectResult() {
    }
    
    public CodeCollectResult(String status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public static CodeCollectResult success(String tagNo, Integer currentCount, Integer totalCount, Boolean isPalletFull) {
        CodeCollectResult result = new CodeCollectResult();
        result.setStatus("SUCCESS");
        result.setMessage("采集成功");
        result.setTagNo(tagNo);
        result.setCurrentCount(currentCount);
        result.setTotalCount(totalCount);
        result.setIsPalletFull(isPalletFull);
        return result;
    }
    
    public static CodeCollectResult noCode(String tagNo, Integer currentCount, Integer totalCount, Boolean isPalletFull, String generatedBoxCode) {
        CodeCollectResult result = new CodeCollectResult();
        result.setStatus("NO_CODE");
        result.setMessage("无码（已自动生成系统箱码）");
        result.setTagNo(tagNo);
        result.setCurrentCount(currentCount);
        result.setTotalCount(totalCount);
        result.setIsPalletFull(isPalletFull);
        result.setGeneratedBoxCode(generatedBoxCode);
        return result;
    }
    
    public static CodeCollectResult duplicate(String code) {
        CodeCollectResult result = new CodeCollectResult();
        result.setStatus("DUPLICATE");
        result.setMessage("重复码：" + code);
        return result;
    }
    
    public static CodeCollectResult invalid(String code) {
        CodeCollectResult result = new CodeCollectResult();
        result.setStatus("INVALID");
        result.setMessage("错码（格式不正确）：" + code);
        return result;
    }
    
    // Getters and Setters
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
    
    public String getGeneratedBoxCode() {
        return generatedBoxCode;
    }
    
    public void setGeneratedBoxCode(String generatedBoxCode) {
        this.generatedBoxCode = generatedBoxCode;
    }
    
    @Override
    public String toString() {
        return "CodeCollectResult{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", tagNo='" + tagNo + '\'' +
                ", currentCount=" + currentCount +
                ", totalCount=" + totalCount +
                ", isPalletFull=" + isPalletFull +
                '}';
    }
}

