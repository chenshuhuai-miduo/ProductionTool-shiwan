package com.miduo.cloud.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 用于系统启动时的异步初始化任务
 * 
 * @author miduo
 * @date 2024-11-24
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 异步任务执行器
     * 专门用于 Bloom Filter 初始化等耗时任务
     */
    @Bean(name = "bloomFilterInitExecutor")
    public Executor bloomFilterInitExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：2（预留给 Bloom Filter 初始化和其他异步任务）
        executor.setCorePoolSize(2);
        
        // 最大线程数：4
        executor.setMaxPoolSize(4);
        
        // 队列容量：50
        executor.setQueueCapacity(50);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("BloomFilter-Init-");
        
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间：60秒
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
    
    /**
     * 通用异步任务执行器
     * 用于其他一般性异步任务
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：根据 CPU 核心数动态调整
        int processors = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(processors);
        
        // 最大线程数：CPU 核心数 * 2
        executor.setMaxPoolSize(processors * 2);
        
        // 队列容量：100
        executor.setQueueCapacity(100);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("Async-Task-");
        
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间：60秒
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
}

