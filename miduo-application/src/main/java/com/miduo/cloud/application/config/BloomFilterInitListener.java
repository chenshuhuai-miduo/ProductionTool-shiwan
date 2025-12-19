package com.miduo.cloud.application.config;

import com.miduo.cloud.application.code.CodeApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 布隆过滤器初始化监听器
 * 监听应用就绪事件，延迟初始化布隆过滤器，避免阻塞系统启动
 * 
 * 功能：
 * 1. 应用就绪后延迟3秒开始初始化布隆过滤器
 * 2. 确保后端可以立即启动完成，不等待布隆过滤器加载
 * 3. 布隆过滤器在后台异步加载，不影响系统使用
 * 
 * @author miduo
 * @date 2024-12-16
 */
@Slf4j
@Component
public class BloomFilterInitListener {
    
    @Autowired
    private CodeApplicationService codeApplicationService;
    
    /**
     * 监听应用就绪事件
     * 延迟3秒后开始初始化布隆过滤器，确保前端可以立即启动
     * 
     * @param event 应用就绪事件
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("[BloomFilter] 应用已就绪，延迟3秒后开始初始化布隆过滤器...");
        
        // 延迟3秒，确保前端可以立即启动
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                log.info("[BloomFilter] 开始异步初始化布隆过滤器...");
                codeApplicationService.initGlobalBoxCodeBloomFilter();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[BloomFilter] 延迟初始化被中断");
            } catch (Exception e) {
                log.error("[BloomFilter] 延迟初始化失败", e);
            }
        }, "BloomFilter-Delayed-Init").start();
    }
}

