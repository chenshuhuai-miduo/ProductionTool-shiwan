package com.miduo.cloud.config;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.util.Set;

/**
 * 石湾 2 号机后端组件扫描排除过滤器。
 * <p>
 * 按全限定类名排除以下 Bean，避免在石湾 2 号机中加载：
 * </p>
 * <ul>
 *   <li>{@code com.miduo.cloud.application.code.CodeBloomFilterManager} — 不初始化全局箱码 Bloom Filter</li>
 *   <li>{@code com.miduo.cloud.domain.code.service.CodeDomainService} — 致美斋码域服务</li>
 * </ul>
 * <p>
 * 使用类名字符串比较，不在注解中引用这些类，从而在它们不在 classpath 时也能正常启动。
 * </p>
 */
public class ShiwanM2ExcludeFilter implements TypeFilter {

    private static final Set<String> EXCLUDED_CLASSES = Set.of(
            "com.miduo.cloud.application.code.CodeBloomFilterManager",
            "com.miduo.cloud.domain.code.service.CodeDomainService"
    );

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
        String className = metadataReader.getClassMetadata().getClassName();
        return EXCLUDED_CLASSES.contains(className);
    }
}
