package com.miduo.cloud.common.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页输出结果类
 * 用于所有分页查询接口的统一返回格式
 */
@Data
public class PageOutput<T> {
    
    /**
     * 当前页码
     */
    private Long current;
    
    /**
     * 每页条数
     */
    private Long size;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 总页数
     */
    private Long pages;
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    public PageOutput() {
    }
    
    public PageOutput(Long current, Long size, Long total, List<T> records) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.pages = (total + size - 1) / size;
        this.records = records;
    }
}

