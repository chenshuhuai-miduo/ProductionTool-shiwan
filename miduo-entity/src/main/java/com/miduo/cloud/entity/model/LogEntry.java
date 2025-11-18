package com.miduo.cloud.entity.model;

import com.miduo.cloud.entity.enums.LogLevel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志条目实体
 */
public class LogEntry {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * 日志时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 日志级别
     */
    private LogLevel level;
    
    /**
     * 模块名称
     */
    private String module;
    
    /**
     * 日志消息
     */
    private String message;
    
    /**
     * 异常堆栈信息
     */
    private String exception;
    
    /**
     * 线程名称
     */
    private String threadName;
    
    public LogEntry() {
        this.timestamp = LocalDateTime.now();
        this.threadName = Thread.currentThread().getName();
    }
    
    public LogEntry(LogLevel level, String module, String message) {
        this();
        this.level = level;
        this.module = module;
        this.message = message;
    }
    
    public LogEntry(LogLevel level, String module, String message, Throwable throwable) {
        this(level, module, message);
        if (throwable != null) {
            this.exception = getStackTraceAsString(throwable);
        }
    }
    
    /**
     * 将异常堆栈转换为字符串
     */
    private String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        
        // 处理 cause
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            sb.append("Caused by: ").append(getStackTraceAsString(cause));
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 转换为 INI 格式字符串
     */
    public String toIniFormat() {
        StringBuilder sb = new StringBuilder();
        
        // 使用时间戳作为 section 名称
        sb.append("[").append(timestamp.format(FORMATTER)).append("]\n");
        sb.append("Level=").append(level.getCode()).append("\n");
        
        if (module != null && !module.isEmpty()) {
            sb.append("Module=").append(module).append("\n");
        }
        
        if (message != null && !message.isEmpty()) {
            sb.append("Message=").append(message.replace("\n", "\\n")).append("\n");
        }
        
        if (exception != null && !exception.isEmpty()) {
            sb.append("Exception=").append(exception.replace("\n", "\n    ")).append("\n");
        }
        
        sb.append("Thread=").append(threadName).append("\n");
        sb.append("\n");
        
        return sb.toString();
    }
    
    // Getters and Setters
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public LogLevel getLevel() {
        return level;
    }
    
    public void setLevel(LogLevel level) {
        this.level = level;
    }
    
    public String getModule() {
        return module;
    }
    
    public void setModule(String module) {
        this.module = module;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getException() {
        return exception;
    }
    
    public void setException(String exception) {
        this.exception = exception;
    }
    
    public String getThreadName() {
        return threadName;
    }
    
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
}

