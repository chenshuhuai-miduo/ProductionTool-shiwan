package com.miduo.cloud.entity.dto.code;

import lombok.Data;

/**
 * 无箱码采集结果
 * 
 * @author System
 * @since 2025-10-30
 */
@Data
public class NoBoxCollectResult {
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 生成的箱码数量
     */
    private Integer generatedCount;
    
    /**
     * 托盘码
     */
    private String palletCode;
    
    /**
     * 虚拟垛标
     */
    private String virtualPalletCode;
    
    /**
     * TagNo（标签编号）
     */
    private String tagNo;
    
    /**
     * 成功结果
     */
    public static NoBoxCollectResult success(String message, Integer generatedCount, 
                                              String palletCode, String virtualPalletCode, String tagNo) {
        NoBoxCollectResult result = new NoBoxCollectResult();
        result.setSuccess(true);
        result.setMessage(message);
        result.setGeneratedCount(generatedCount);
        result.setPalletCode(palletCode);
        result.setVirtualPalletCode(virtualPalletCode);
        result.setTagNo(tagNo);
        return result;
    }
    
    /**
     * 失败结果
     */
    public static NoBoxCollectResult fail(String message) {
        NoBoxCollectResult result = new NoBoxCollectResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}

