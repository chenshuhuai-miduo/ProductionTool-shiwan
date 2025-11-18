package com.miduo.cloud.frontend.util;

import com.miduo.cloud.entity.enums.LogLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * 日志重定向器
 * 将 System.out 和 System.err 的输出同时重定向到文件和控制台
 */
public class LogRedirector extends PrintStream {
    
    private final PrintStream originalStream;
    private final LogLevel logLevel;
    private final ByteArrayOutputStream buffer;
    
    /**
     * 构造函数
     * 
     * @param originalStream 原始输出流（用于保留控制台输出）
     * @param logLevel 日志级别（INFO 或 ERROR）
     */
    public LogRedirector(PrintStream originalStream, LogLevel logLevel) {
        super(new ByteArrayOutputStream());
        this.originalStream = originalStream;
        this.logLevel = logLevel;
        this.buffer = (ByteArrayOutputStream) this.out;
    }
    
    @Override
    public void write(int b) {
        // 同时写入原始流（控制台）
        originalStream.write(b);
        
        // 缓冲字节
        buffer.write(b);
        
        // 如果是换行符，处理缓冲区
        if (b == '\n') {
            flushBuffer();
        }
    }
    
    @Override
    public void write(byte[] buf, int off, int len) {
        // 同时写入原始流（控制台）
        originalStream.write(buf, off, len);
        
        // 写入缓冲区
        buffer.write(buf, off, len);
        
        // 检查是否包含换行符
        for (int i = off; i < off + len; i++) {
            if (buf[i] == '\n') {
                flushBuffer();
                break;
            }
        }
    }
    
    @Override
    public void flush() {
        originalStream.flush();
        flushBuffer();
    }
    
    /**
     * 刷新缓冲区并写入文件日志
     */
    private void flushBuffer() {
        if (buffer.size() == 0) {
            return;
        }
        
        try {
            String line = buffer.toString(StandardCharsets.UTF_8.name()).trim();
            
            // 如果是空行，忽略
            if (line.isEmpty()) {
                buffer.reset();
                return;
            }
            
            // 解析模块名称和消息
            String module = "系统输出";
            String message = line;
            
            // 尝试从日志格式中提取模块名称
            // 格式示例：[HH:mm:ss.SSS] [LEVEL] [Module] Message
            if (line.startsWith("[") && line.contains("]")) {
                int firstClose = line.indexOf(']');
                if (firstClose > 0 && line.length() > firstClose + 1) {
                    String remaining = line.substring(firstClose + 1).trim();
                    
                    // 检查是否有第二个中括号（级别）
                    if (remaining.startsWith("[") && remaining.contains("]")) {
                        int secondClose = remaining.indexOf(']');
                        if (secondClose > 0 && remaining.length() > secondClose + 1) {
                            String remaining2 = remaining.substring(secondClose + 1).trim();
                            
                            // 检查是否有第三个中括号（模块）
                            if (remaining2.startsWith("[") && remaining2.contains("]")) {
                                int thirdClose = remaining2.indexOf(']');
                                if (thirdClose > 0) {
                                    module = remaining2.substring(1, thirdClose);
                                    if (remaining2.length() > thirdClose + 1) {
                                        message = remaining2.substring(thirdClose + 1).trim();
                                    }
                                }
                            } else {
                                message = remaining2;
                            }
                        }
                    } else {
                        message = remaining;
                    }
                }
            }
            
            // 智能识别日志级别
            LogLevel actualLevel = detectLogLevel(line, logLevel);
            
            // 写入文件日志
            if (actualLevel == LogLevel.ERROR) {
                FileLogManager.getInstance().logError(module, message);
            } else if (actualLevel == LogLevel.WARN) {
                FileLogManager.getInstance().logWarn(module, message);
            } else if (actualLevel == LogLevel.DEBUG) {
                FileLogManager.getInstance().logDebug(module, message);
            } else {
                FileLogManager.getInstance().logInfo(module, message);
            }
            
        } catch (Exception e) {
            // 避免在日志记录过程中抛出异常
            originalStream.println("[LogRedirector] 写入日志失败: " + e.getMessage());
        } finally {
            buffer.reset();
        }
    }
    
    /**
     * 智能识别日志级别
     * 
     * @param message 日志消息
     * @param defaultLevel 默认级别
     * @return 识别后的日志级别
     */
    private LogLevel detectLogLevel(String message, LogLevel defaultLevel) {
        String lowerMessage = message.toLowerCase();
        
        // 检查错误关键字
        if (lowerMessage.contains("error") || 
            lowerMessage.contains("错误") || 
            lowerMessage.contains("exception") || 
            lowerMessage.contains("异常") || 
            lowerMessage.contains("failed") || 
            lowerMessage.contains("失败") ||
            lowerMessage.contains("✗")) {
            return LogLevel.ERROR;
        }
        
        // 检查警告关键字
        if (lowerMessage.contains("warn") || 
            lowerMessage.contains("warning") || 
            lowerMessage.contains("警告") || 
            lowerMessage.contains("caution") ||
            lowerMessage.contains("注意")) {
            return LogLevel.WARN;
        }
        
        // 检查调试关键字
        if (lowerMessage.contains("debug") || 
            lowerMessage.contains("调试") ||
            lowerMessage.contains("trace")) {
            return LogLevel.DEBUG;
        }
        
        // 使用默认级别
        return defaultLevel;
    }
    
    /**
     * 安装日志重定向器
     */
    public static void install() {
        try {
            // 保存原始流
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            
            // 创建重定向流
            LogRedirector outRedirector = new LogRedirector(originalOut, LogLevel.INFO);
            LogRedirector errRedirector = new LogRedirector(originalErr, LogLevel.ERROR);
            
            // 替换系统输出流
            System.setOut(outRedirector);
            System.setErr(errRedirector);
            
            System.out.println("[日志重定向] System.out 和 System.err 已重定向到文件日志");
            FileLogManager.getInstance().logInfo("日志重定向", "System.out 和 System.err 已重定向到文件日志");
            
        } catch (Exception e) {
            System.err.println("[日志重定向] 安装失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

