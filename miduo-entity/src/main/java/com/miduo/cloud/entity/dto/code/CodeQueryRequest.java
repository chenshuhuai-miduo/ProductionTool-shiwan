package com.miduo.cloud.entity.dto.code;

import lombok.Data;

/**
 * 码查询请求DTO
 */
@Data
public class CodeQueryRequest {
    
    /**
     * 查询的码值（SmallSerialNumber）
     */
    private String code;
}

