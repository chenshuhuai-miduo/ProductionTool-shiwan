package com.miduo.cloud.frontend.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 文件日志工具类
 * 用于记录系统日志和异常日志到 .ini 文件
 * 
 * 文件组织结构：
 * 项目根目录/logs/年月/日.ini
 * 例如：D:/javaspace/miduo-ccas/logs/2025年11月/2025-11-04.ini
 * 
 * @author System
 * @since 2025-10-30
 */
public class FileLogUtil {
    
    /**
     * 日志根目录
     */
    private static final String LOG_ROOT_DIR = "logs";
    
    /**
     * 日期格式化 - 年月文件夹
     */
    private static final DateTimeFormatter FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy年MM月");
    
    /**
     * 日期格式化 - 日文件名
     */
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 时间格式化 - 日志时间戳
     */
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * 文件写入锁（保证并发安全）
     */
    private static final ReentrantLock WRITE_LOCK = new ReentrantLock();
    
    /**
     * 日志级别
     */
    public enum LogLevel {
        INFO, WARN, ERROR, DEBUG
    }
    
    /**
     * 记录 INFO 级别日志
     * 
     * @param module 模块名称
     * @param message 日志消息
     */
    public static void info(String module, String message) {
        writeLog(LogLevel.INFO, module, message, null);
    }
    
    /**
     * 记录 WARN 级别日志
     * 
     * @param module 模块名称
     * @param message 日志消息
     */
    public static void warn(String module, String message) {
        writeLog(LogLevel.WARN, module, message, null);
    }
    
    /**
     * 记录 ERROR 级别日志
     * 
     * @param module 模块名称
     * @param message 日志消息
     * @param exception 异常对象
     */
    public static void error(String module, String message, Exception exception) {
        writeLog(LogLevel.ERROR, module, message, exception);
    }
    
    /**
     * 记录 DEBUG 级别日志
     * 
     * @param module 模块名称
     * @param message 日志消息
     */
    public static void debug(String module, String message) {
        writeLog(LogLevel.DEBUG, module, message, null);
    }
    
    /**
     * 写入日志到文件
     * 
     * @param level 日志级别
     * @param module 模块名称
     * @param message 日志消息
     * @param exception 异常对象（可选）
     */
    private static void writeLog(LogLevel level, String module, String message, Exception exception) {
        WRITE_LOCK.lock();
        try {
            // 1. 获取当前日期时间
            LocalDateTime now = LocalDateTime.now();
            
            // 2. 构建日志文件路径
            Path logFilePath = getLogFilePath(now);
            
            // 3. 确保目录存在
            ensureDirectoryExists(logFilePath.getParent());
            
            // 4. 构建日志内容
            String logContent = buildLogContent(level, module, message, exception, now);
            
            // 5. 写入文件（追加模式）
            Files.write(logFilePath, logContent.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            // 6. 控制台输出（方便调试）
            System.out.println("[文件日志] 已写入: " + logFilePath.toString());
            
        } catch (IOException e) {
            // 文件日志写入失败，输出到控制台
            System.err.println("[文件日志] 写入失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            WRITE_LOCK.unlock();
        }
    }
    
    /**
     * 获取日志文件路径
     * 格式：项目根目录/logs/年月/日.ini
     * 
     * @param dateTime 日期时间
     * @return 日志文件路径
     */
    private static Path getLogFilePath(LocalDateTime dateTime) {
        // 项目根目录
        String projectRoot = System.getProperty("user.dir");
        
        // 年月文件夹名称（例如：2025年11月）
        String folderName = dateTime.format(FOLDER_FORMAT);
        
        // 日文件名（例如：2025-11-04.ini）
        String fileName = dateTime.format(FILE_FORMAT) + ".ini";
        
        // 完整路径
        return Paths.get(projectRoot, LOG_ROOT_DIR, folderName, fileName);
    }
    
    /**
     * 确保目录存在，不存在则创建
     * 
     * @param dirPath 目录路径
     */
    private static void ensureDirectoryExists(Path dirPath) throws IOException {
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            System.out.println("[文件日志] 创建目录: " + dirPath.toString());
        }
    }
    
    /**
     * 构建日志内容（.ini 格式）
     * 
     * @param level 日志级别
     * @param module 模块名称
     * @param message 日志消息
     * @param exception 异常对象
     * @param dateTime 日期时间
     * @return 日志内容字符串
     */
    private static String buildLogContent(LogLevel level, String module, String message,
                                           Exception exception, LocalDateTime dateTime) {
        StringBuilder sb = new StringBuilder();
        
        // 日志分隔符（每条日志用 [时间戳] 开始）
        sb.append("\n[").append(dateTime.format(TIMESTAMP_FORMAT)).append("]\n");
        
        // 日志级别
        sb.append("Level=").append(level.name()).append("\n");
        
        // 模块名称
        sb.append("Module=").append(module != null ? module : "未知模块").append("\n");
        
        // 日志消息
        sb.append("Message=").append(message != null ? message : "").append("\n");
        
        // 如果有异常，记录异常信息
        if (exception != null) {
            sb.append("Exception=").append(exception.getClass().getName())
              .append(": ").append(exception.getMessage()).append("\n");
            
            // 异常堆栈
            sb.append("StackTrace=\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            sb.append(sw.toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 清理旧日志文件
     * 删除超过指定天数的日志文件
     * 
     * @param daysToKeep 保留天数（例如：30 表示保留最近 30 天）
     */
    public static void cleanOldLogs(int daysToKeep) {
        try {
            String projectRoot = System.getProperty("user.dir");
            Path logsDir = Paths.get(projectRoot, LOG_ROOT_DIR);
            
            if (!Files.exists(logsDir)) {
                return;
            }
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
            
            // 遍历所有年月文件夹
            Files.walk(logsDir, 2)
                 .filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".ini"))
                 .forEach(path -> {
                     try {
                         // 从文件名解析日期
                         String fileName = path.getFileName().toString().replace(".ini", "");
                         LocalDateTime fileDate = LocalDateTime.parse(fileName + " 00:00:00",
                                 DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                         
                         // 如果文件日期早于截止日期，删除
                         if (fileDate.isBefore(cutoffDate)) {
                             Files.delete(path);
                             System.out.println("[文件日志] 清理旧日志: " + path.toString());
                         }
                     } catch (Exception e) {
                         System.err.println("[文件日志] 清理日志失败: " + e.getMessage());
                     }
                 });
            
            System.out.println("[文件日志] 旧日志清理完成");
            
        } catch (Exception e) {
            System.err.println("[文件日志] 清理旧日志异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查单个日志文件大小
     * 如果文件过大，可以考虑切分或归档
     * 
     * @param filePath 文件路径
     * @return 文件大小（字节）
     */
    public static long getFileSize(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }
        } catch (IOException e) {
            System.err.println("[文件日志] 获取文件大小失败: " + e.getMessage());
        }
        return 0;
    }
}

