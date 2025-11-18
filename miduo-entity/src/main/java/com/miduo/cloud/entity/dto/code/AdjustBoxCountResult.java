package com.miduo.cloud.entity.dto.code;

import java.util.List;

/**
 * 调整当前箱数结果
 */
public class AdjustBoxCountResult {
    
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
     * 调整前的数量
     */
    private Integer beforeCount;
    
    /**
     * 调整后的数量
     */
    private Integer afterCount;
    
    /**
     * 操作类型：DELETE-删除, ADD-添加, NONE-无需调整
     */
    private String operationType;
    
    /**
     * 影响的记录数（删除或添加的数量）
     */
    private Integer affectedCount;
    
    /**
     * 生成的虚拟箱码列表（仅在ADD操作时有值）
     */
    private List<String> generatedCodes;
    
    public static AdjustBoxCountResult success(String tagNo, Integer beforeCount, Integer afterCount, 
                                               String operationType, Integer affectedCount, String message) {
        AdjustBoxCountResult result = new AdjustBoxCountResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setTagNo(tagNo);
        result.setBeforeCount(beforeCount);
        result.setAfterCount(afterCount);
        result.setOperationType(operationType);
        result.setAffectedCount(affectedCount);
        return result;
    }
    
    public static AdjustBoxCountResult success(String tagNo, Integer beforeCount, Integer afterCount, 
                                               String operationType, Integer affectedCount, String message, 
                                               List<String> generatedCodes) {
        AdjustBoxCountResult result = new AdjustBoxCountResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setTagNo(tagNo);
        result.setBeforeCount(beforeCount);
        result.setAfterCount(afterCount);
        result.setOperationType(operationType);
        result.setAffectedCount(affectedCount);
        result.setGeneratedCodes(generatedCodes);
        return result;
    }
    
    public static AdjustBoxCountResult error(String message) {
        AdjustBoxCountResult result = new AdjustBoxCountResult();
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
    
    public Integer getBeforeCount() {
        return beforeCount;
    }
    
    public void setBeforeCount(Integer beforeCount) {
        this.beforeCount = beforeCount;
    }
    
    public Integer getAfterCount() {
        return afterCount;
    }
    
    public void setAfterCount(Integer afterCount) {
        this.afterCount = afterCount;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    public Integer getAffectedCount() {
        return affectedCount;
    }
    
    public void setAffectedCount(Integer affectedCount) {
        this.affectedCount = affectedCount;
    }
    
    public List<String> getGeneratedCodes() {
        return generatedCodes;
    }
    
    public void setGeneratedCodes(List<String> generatedCodes) {
        this.generatedCodes = generatedCodes;
    }
    
    @Override
    public String toString() {
        return "AdjustBoxCountResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", tagNo='" + tagNo + '\'' +
                ", beforeCount=" + beforeCount +
                ", afterCount=" + afterCount +
                ", operationType='" + operationType + '\'' +
                ", affectedCount=" + affectedCount +
                ", generatedCodes=" + generatedCodes +
                '}';
    }
}

