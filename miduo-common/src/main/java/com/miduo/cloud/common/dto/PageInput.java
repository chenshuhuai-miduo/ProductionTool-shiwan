package com.miduo.cloud.common.dto;

import lombok.Data;

/**
 * 分页输入参数类
 * 用于所有分页查询接口的统一输入参数
 */
@Data
public class PageInput {
    
    /**
     * 当前页码（默认第1页）
     */
    private Long current = 1L;
    
    /**
     * 每页条数（默认20条）
     */
    private Long size = 20L;
    
    /**
     * 排序字段
     */
    private String sortField;
    
    /**
     * 排序方式（asc/desc）
     */
    private String sortOrder;
}

