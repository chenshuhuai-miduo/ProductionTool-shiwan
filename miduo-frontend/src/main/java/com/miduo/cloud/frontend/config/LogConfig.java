package com.miduo.cloud.frontend.config;

import java.io.File;

/**
 * 日志配置类
 */
public class LogConfig {
    
    /**
     * 日志根目录（系统根目录下的 logs 文件夹）
     */
    private static final String LOG_ROOT_DIR;
    
    /**
     * 日志文件编码
     */
    private static final String LOG_ENCODING = "UTF-8";
    
    /**
     * 是否启用异步写入
     */
    private static final boolean ASYNC_ENABLED = true;
    
    /**
     * 异步队列大小
     */
    private static final int ASYNC_QUEUE_SIZE = 1000;
    
    /**
     * 是否同时输出到控制台
     */
    private static final boolean CONSOLE_OUTPUT = true;
    
    static {
        // 获取系统根目录
        String userDir = System.getProperty("user.dir");
        LOG_ROOT_DIR = userDir + File.separator + "logs";
        
        // 确保日志根目录存在
        File logDir = new File(LOG_ROOT_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }
    
    public static String getLogRootDir() {
        return LOG_ROOT_DIR;
    }
    
    public static String getLogEncoding() {
        return LOG_ENCODING;
    }
    
    public static boolean isAsyncEnabled() {
        return ASYNC_ENABLED;
    }
    
    public static int getAsyncQueueSize() {
        return ASYNC_QUEUE_SIZE;
    }
    
    public static boolean isConsoleOutput() {
        return CONSOLE_OUTPUT;
    }
    
    /**
     * 获取当前年月的日志目录
     * @param year 年份
     * @param month 月份（1-12）
     * @return 日志目录路径
     */
    public static String getYearMonthDir(int year, int month) {
        return LOG_ROOT_DIR + File.separator + String.format("%04d-%02d", year, month);
    }
    
    /**
     * 获取当天的日志文件路径
     * @param year 年份
     * @param month 月份（1-12）
     * @param day 日期（1-31）
     * @return 日志文件完整路径
     */
    public static String getDailyLogFilePath(int year, int month, int day) {
        String yearMonthDir = getYearMonthDir(year, month);
        return yearMonthDir + File.separator + String.format("%04d-%02d-%02d.ini", year, month, day);
    }
}

