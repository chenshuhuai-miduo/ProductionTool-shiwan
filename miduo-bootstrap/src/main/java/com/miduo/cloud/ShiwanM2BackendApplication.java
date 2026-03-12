package com.miduo.cloud;

import com.miduo.cloud.config.ShiwanM2ExcludeFilter;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 石湾 2 号机专用后端启动类。
 * <p>
 * 与 {@link MiduoBackendApplication} 的区别：
 * </p>
 * <ul>
 *   <li>不扫描 {@code com.miduo.cloud.variant.zhimeizhai}，因此不会加载致美斋的
 *       {@code CodeApplicationService}、{@code BloomFilterInitListener} 及致美斋 Mapper。</li>
 *   <li>通过 {@link ShiwanM2ExcludeFilter} 排除 {@code CodeBloomFilterManager} 与
 *       {@code CodeDomainService}，避免“初始化全局箱码 Bloom Filter”及致美斋码域服务。</li>
 *   <li>不扫描致美斋 Mapper 包，仅保留基础设施层及石湾 2 号机所需的数据访问。</li>
 * </ul>
 * <p>
 * 仍保留：Tomcat 8080、数据源、MyBatis、controller/application/domain/infrastructure/config、
 * 定时任务（如 M1 同步）、石湾 2 号机相关 API。
 * </p>
 */
@EnableScheduling
@SpringBootApplication
@ComponentScan(
    basePackages = {
        "com.miduo.cloud.controller",
        "com.miduo.cloud.application",
        "com.miduo.cloud.domain",
        "com.miduo.cloud.infrastructure",
        "com.miduo.cloud.config"
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = ShiwanM2ExcludeFilter.class)
)
@MapperScan({
    "com.miduo.cloud.infrastructure.mapper",
    "com.miduo.cloud.infrastructure.persistence.mybatis.mapper"
})
public class ShiwanM2BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiwanM2BackendApplication.class, args);
        System.out.println("========================================");
        System.out.println("石湾 2 号机后端启动成功！");
        System.out.println("API地址：http://localhost:8080/api");
        System.out.println("========================================");
    }
}
