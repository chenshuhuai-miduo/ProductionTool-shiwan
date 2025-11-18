package com.miduo.cloud.entity.dto.code;

import lombok.Data;

/**
 * 码替换请求DTO
 */
@Data
public class CodeReplaceRequest {
    
    /**
     * 原码（旧码）
     */
    private String oldCode;
    
    /**
     * 新码
     */
    private String newCode;
    
    /**
     * 替换原因
     */
    private String reason;
}

