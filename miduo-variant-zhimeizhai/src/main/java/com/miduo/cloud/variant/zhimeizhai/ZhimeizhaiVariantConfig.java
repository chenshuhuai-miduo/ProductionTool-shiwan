package com.miduo.cloud.variant.zhimeizhai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 致美斋变体模块配置。
 * 确保 Spring 扫描到 com.miduo.cloud.variant.zhimeizhai 包，
 * 并扫描致美斋专属 Mapper。
 */
@Configuration
@MapperScan("com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.mapper")
public class ZhimeizhaiVariantConfig {
}
