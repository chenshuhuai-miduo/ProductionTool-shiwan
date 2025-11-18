package com.miduo.cloud.entity.dto.code;

/**
 * 生成系统箱码结果
 */
public class GenerateSystemCodeResult {
    
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
     * 生成的系统箱码
     */
    private String generatedCode;
    
    /**
     * 更新前的状态（无码）
     */
    private Boolean wasEmpty;
    
    /**
     * 当前箱数（保持不变）
     */
    private Integer currentCount;
    
    public static GenerateSystemCodeResult success(String tagNo, String generatedCode, 
                                                   Integer currentCount, String message) {
        GenerateSystemCodeResult result = new GenerateSystemCodeResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setTagNo(tagNo);
        result.setGeneratedCode(generatedCode);
        result.setWasEmpty(true);
        result.setCurrentCount(currentCount);
        return result;
    }
    
    public static GenerateSystemCodeResult error(String message) {
        GenerateSystemCodeResult result = new GenerateSystemCodeResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public boolean isSuccess() {
        return success != null && success;
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
    
    public String getGeneratedCode() {
        return generatedCode;
    }
    
    public void setGeneratedCode(String generatedCode) {
        this.generatedCode = generatedCode;
    }
    
    public Boolean getWasEmpty() {
        return wasEmpty;
    }
    
    public void setWasEmpty(Boolean wasEmpty) {
        this.wasEmpty = wasEmpty;
    }
    
    public Integer getCurrentCount() {
        return currentCount;
    }
    
    public void setCurrentCount(Integer currentCount) {
        this.currentCount = currentCount;
    }
    
    @Override
    public String toString() {
        return "GenerateSystemCodeResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", tagNo='" + tagNo + '\'' +
                ", generatedCode='" + generatedCode + '\'' +
                ", wasEmpty=" + wasEmpty +
                ", currentCount=" + currentCount +
                '}';
    }
}

