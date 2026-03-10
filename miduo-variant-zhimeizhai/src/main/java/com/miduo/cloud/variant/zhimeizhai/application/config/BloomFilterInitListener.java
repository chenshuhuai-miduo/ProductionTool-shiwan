package com.miduo.cloud.variant.zhimeizhai.application.config;

import com.miduo.cloud.variant.zhimeizhai.application.code.CodeApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 布隆过滤器初始化监听器（致美斋产线）
 */
@Slf4j
@Component
public class BloomFilterInitListener {

    @Autowired
    private CodeApplicationService codeApplicationService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("[BloomFilter] 应用已就绪，延迟3秒后开始初始化布隆过滤器...");
        Thread delayThread = new Thread(() -> {
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
        }, "BloomFilter-Delayed-Init");
        delayThread.setDaemon(true);
        delayThread.start();
    }
}
