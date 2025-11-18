package com.miduo.cloud.entity.enums;

/**
 * 日志级别枚举
 */
public enum LogLevel {
    
    /**
     * 调试级别 - 详细的调试信息
     */
    DEBUG("DEBUG", "调试"),
    
    /**
     * 信息级别 - 一般信息
     */
    INFO("INFO", "信息"),
    
    /**
     * 警告级别 - 警告信息
     */
    WARN("WARN", "警告"),
    
    /**
     * 错误级别 - 错误信息
     */
    ERROR("ERROR", "错误");
    
    private final String code;
    private final String description;
    
    LogLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return code;
    }
}

