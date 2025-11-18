package com.miduo.cloud.entity.dto.code;

/**
 * 读码剔除校验结果
 */
public class CodeRejectResult {
    
    /**
     * 校验结果：01-合格放行, 02-剔除
     */
    private String result;
    
    /**
     * 剔除原因：NO_CODE-无码, DUPLICATE-重码, INVALID_FORMAT-格式错误, ALL_LETTERS-全字母
     */
    private String rejectReason;
    
    /**
     * 消息
     */
    private String message;
    
    public static CodeRejectResult pass() {
        CodeRejectResult result = new CodeRejectResult();
        result.setResult("01");
        result.setMessage("合格，放行");
        return result;
    }
    
    public static CodeRejectResult reject(String reason, String message) {
        CodeRejectResult result = new CodeRejectResult();
        result.setResult("02");
        result.setRejectReason(reason);
        result.setMessage(message);
        return result;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public String getRejectReason() {
        return rejectReason;
    }
    
    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

