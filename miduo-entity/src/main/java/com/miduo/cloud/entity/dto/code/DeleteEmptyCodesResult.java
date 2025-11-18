package com.miduo.cloud.entity.dto.code;

/**
 * 删除本垛无码结果
 */
public class DeleteEmptyCodesResult {
    
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
     * 删除前的箱数
     */
    private Integer beforeCount;
    
    /**
     * 删除的无码数量
     */
    private Integer deletedCount;
    
    /**
     * 删除后的箱数
     */
    private Integer afterCount;
    
    public static DeleteEmptyCodesResult success(String tagNo, Integer beforeCount, 
                                                 Integer deletedCount, Integer afterCount, String message) {
        DeleteEmptyCodesResult result = new DeleteEmptyCodesResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setTagNo(tagNo);
        result.setBeforeCount(beforeCount);
        result.setDeletedCount(deletedCount);
        result.setAfterCount(afterCount);
        return result;
    }
    
    public static DeleteEmptyCodesResult error(String message) {
        DeleteEmptyCodesResult result = new DeleteEmptyCodesResult();
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
    
    public Integer getDeletedCount() {
        return deletedCount;
    }
    
    public void setDeletedCount(Integer deletedCount) {
        this.deletedCount = deletedCount;
    }
    
    public Integer getAfterCount() {
        return afterCount;
    }
    
    public void setAfterCount(Integer afterCount) {
        this.afterCount = afterCount;
    }
    
    @Override
    public String toString() {
        return "DeleteEmptyCodesResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", tagNo='" + tagNo + '\'' +
                ", beforeCount=" + beforeCount +
                ", deletedCount=" + deletedCount +
                ", afterCount=" + afterCount +
                '}';
    }
}

