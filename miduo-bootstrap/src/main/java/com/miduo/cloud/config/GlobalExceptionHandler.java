package com.miduo.cloud.config;

import com.miduo.cloud.frontend.util.FileLogManager;

/**
 * 全局异常处理器
 * 捕获未处理的异常并记录到日志文件
 */
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    
    private final Thread.UncaughtExceptionHandler originalHandler;
    
    public GlobalExceptionHandler() {
        this.originalHandler = Thread.getDefaultUncaughtExceptionHandler();
    }
    
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            // 记录异常到文件日志
            FileLogManager.getInstance().logError(
                "全局异常捕获", 
                "线程 [" + t.getName() + "] 发生未捕获异常: " + e.getMessage(),
                e
            );
            
            // 同时输出到控制台
            System.err.println("========================================");
            System.err.println("全局异常捕获 - 线程: " + t.getName());
            System.err.println("异常信息: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            
        } catch (Exception ex) {
            System.err.println("[全局异常处理器] 记录异常失败: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        // 调用原始的异常处理器
        if (originalHandler != null) {
            originalHandler.uncaughtException(t, e);
        }
    }
    
    /**
     * 安装全局异常处理器
     */
    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
        System.out.println("[全局异常处理器] 已安装");
    }
}

