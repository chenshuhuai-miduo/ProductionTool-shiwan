package com.miduo.cloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 米多星球后端服务启动类
 * 
 * @SpringBootApplication 包含了以下三个注解：
 * - @SpringBootConfiguration：标识为配置类
 * - @EnableAutoConfiguration：启用自动配置
 * - @ComponentScan：组件扫描
 * 
 * scanBasePackages 指定扫描的包路径，包括：
 * - com.miduo.cloud.controller：控制器层
 * - com.miduo.cloud.application：应用服务层
 * - com.miduo.cloud.domain：领域服务层
 * - com.miduo.cloud.infrastructure：基础设施层
 * - com.miduo.cloud.config：配置类
 */
@SpringBootApplication(scanBasePackages = {
    "com.miduo.cloud.controller",
    "com.miduo.cloud.application",
    "com.miduo.cloud.domain",
    "com.miduo.cloud.infrastructure",
    "com.miduo.cloud.config"
})
@MapperScan({
    "com.miduo.cloud.infrastructure.mapper",
    "com.miduo.cloud.infrastructure.persistence.mybatis.mapper"
})
public class MiduoBackendApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MiduoBackendApplication.class, args);
        System.out.println("========================================");
        System.out.println("米多星球后端服务启动成功！");
        System.out.println("API地址：http://localhost:8080/api");
        System.out.println("========================================");
    }
}

