package com.miduo.cloud.frontend.util;

import com.miduo.cloud.entity.enums.LogLevel;
import com.miduo.cloud.entity.model.LogEntry;
import com.miduo.cloud.frontend.config.LogConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 文件日志管理器
 * 线程安全的日志写入工具，支持异步写入
 */
public class FileLogManager {
    
    private static final FileLogManager INSTANCE = new FileLogManager();
    
    /**
     * 异步日志队列
     */
    private final BlockingQueue<LogEntry> logQueue;
    
    /**
     * 日志写入线程
     */
    private final Thread logWriterThread;
    
    /**
     * 是否正在运行
     */
    private volatile boolean running = false;
    
    private FileLogManager() {
        this.logQueue = new LinkedBlockingQueue<>(LogConfig.getAsyncQueueSize());
        this.logWriterThread = new Thread(this::processLogQueue, "FileLogWriter");
        this.logWriterThread.setDaemon(true);
    }
    
    public static FileLogManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 启动日志管理器
     */
    public synchronized void start() {
        if (!running) {
            running = true;
            logWriterThread.start();
            logInfo("日志管理器", "文件日志系统启动成功，日志根目录: " + LogConfig.getLogRootDir());
        }
    }
    
    /**
     * 停止日志管理器
     */
    public synchronized void stop() {
        if (running) {
            running = false;
            logInfo("日志管理器", "文件日志系统正在关闭...");
            
            // 等待队列中的日志写完
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            logWriterThread.interrupt();
        }
    }
    
    /**
     * 处理日志队列（异步写入线程）
     */
    private void processLogQueue() {
        while (running || !logQueue.isEmpty()) {
            try {
                LogEntry entry = logQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (entry != null) {
                    writeLogToFile(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[日志系统] 写入日志失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 写入日志到文件
     */
    private void writeLogToFile(LogEntry entry) {
        try {
            LocalDate now = LocalDate.now();
            String logFilePath = LogConfig.getDailyLogFilePath(now.getYear(), now.getMonthValue(), now.getDayOfMonth());
            
            // 确保目录存在
            File logFile = new File(logFilePath);
            File parentDir = logFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // 追加写入日志（使用同步块保证线程安全）
            synchronized (this) {
                try (FileOutputStream fos = new FileOutputStream(logFile, true);
                     OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                    writer.write(entry.toIniFormat());
                    writer.flush();
                }
            }
            
            // 同时输出到控制台（如果启用）
            if (LogConfig.isConsoleOutput()) {
                printToConsole(entry);
            }
            
        } catch (IOException e) {
            System.err.println("[日志系统] 无法写入日志文件: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 输出到控制台
     */
    private void printToConsole(LogEntry entry) {
        String prefix = String.format("[%s] [%s] [%s] ", 
            entry.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
            entry.getLevel().getCode(),
            entry.getModule());
        
        if (entry.getLevel() == LogLevel.ERROR) {
            System.err.println(prefix + entry.getMessage());
            if (entry.getException() != null) {
                System.err.println(entry.getException());
            }
        } else {
            System.out.println(prefix + entry.getMessage());
        }
    }
    
    /**
     * 记录日志（异步）
     */
    private void log(LogEntry entry) {
        if (LogConfig.isAsyncEnabled()) {
            // 异步写入
            if (!logQueue.offer(entry)) {
                System.err.println("[日志系统] 日志队列已满，丢弃日志: " + entry.getMessage());
            }
        } else {
            // 同步写入
            writeLogToFile(entry);
        }
    }
    
    // ==================== 公共日志方法 ====================
    
    /**
     * 记录 DEBUG 日志
     */
    public void logDebug(String module, String message) {
        log(new LogEntry(LogLevel.DEBUG, module, message));
    }
    
    /**
     * 记录 INFO 日志
     */
    public void logInfo(String module, String message) {
        log(new LogEntry(LogLevel.INFO, module, message));
    }
    
    /**
     * 记录 WARN 日志
     */
    public void logWarn(String module, String message) {
        log(new LogEntry(LogLevel.WARN, module, message));
    }
    
    /**
     * 记录 WARN 日志（带异常）
     */
    public void logWarn(String module, String message, Throwable throwable) {
        log(new LogEntry(LogLevel.WARN, module, message, throwable));
    }
    
    /**
     * 记录 ERROR 日志
     */
    public void logError(String module, String message) {
        log(new LogEntry(LogLevel.ERROR, module, message));
    }
    
    /**
     * 记录 ERROR 日志（带异常）
     */
    public void logError(String module, String message, Throwable throwable) {
        log(new LogEntry(LogLevel.ERROR, module, message, throwable));
    }
    
    /**
     * 获取当前日志文件路径
     */
    public String getCurrentLogFilePath() {
        LocalDate now = LocalDate.now();
        return LogConfig.getDailyLogFilePath(now.getYear(), now.getMonthValue(), now.getDayOfMonth());
    }
    
    /**
     * 获取日志队列大小
     */
    public int getQueueSize() {
        return logQueue.size();
    }
}

