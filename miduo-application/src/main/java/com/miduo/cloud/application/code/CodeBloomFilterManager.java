package com.miduo.cloud.application.code;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 箱码重复检查 Bloom Filter 管理器
 * 
 * 功能：
 * 1. 维护全局的箱码 Bloom Filter（只存储箱码 SmallSerialNumber）
 * 2. 快速判断箱码是否可能重复（减少80%的数据库查询）
 * 3. 校验整个数据库所有未删除的箱码（不限订单和产品）
 * 
 * 性能优化：
 * - Bloom Filter 误判率：0.01%（万分之一）
 * - 内存占用：约4.5MB（预估200万个箱码）
 * - 查询时间：O(1)，纳秒级
 * 
 * @author miduo
 * @date 2025-11-24
 */
@Slf4j
@Component
public class CodeBloomFilterManager {
    
    /**
     * 全局箱码 Bloom Filter
     * 只存储整个数据库中所有未删除的箱码（SmallSerialNumber）
     */
    private volatile BloomFilter<String> globalBoxCodeFilter;
    
    /**
     * 已删除的箱码集合（用于标记已删除的码，因为布隆过滤器不支持删除操作）
     */
    private final ConcurrentHashMap<String, Boolean> deletedBoxCodes = new ConcurrentHashMap<>();
    
    /**
     * Bloom Filter 配置参数
     */
    private static final int EXPECTED_INSERTIONS = 2000000; // 预期插入码数量：200万
    private static final double FALSE_POSITIVE_PROBABILITY = 0.0001; // 误判率：0.01%
    
    /**
     * 构造函数，初始化全局箱码 Bloom Filter
     */
    public CodeBloomFilterManager() {
        log.info("[BloomFilter] 初始化全局箱码 Bloom Filter");
        this.globalBoxCodeFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            EXPECTED_INSERTIONS,
            FALSE_POSITIVE_PROBABILITY
        );
    }
    
    /**
     * 检查箱码是否可能存在（快速筛选）
     * 注意：校验整个数据库所有未删除的箱码，不限订单和产品
     * 
     * @param boxCode 要检查的箱码
     * @return true=可能存在（需要查数据库确认），false=一定不存在
     */
    public boolean mightContainBoxCode(String boxCode) {
        if (boxCode == null || boxCode.isEmpty()) {
            return false;
        }
        // 如果码已被标记为删除，直接返回false
        if (deletedBoxCodes.containsKey(boxCode)) {
            return false;
        }
        return globalBoxCodeFilter.mightContain(boxCode);
    }
    
    /**
     * 添加箱码到全局 Bloom Filter
     * 
     * @param boxCode 要添加的箱码
     */
    public void putBoxCode(String boxCode) {
        if (boxCode == null || boxCode.isEmpty()) {
            return;
        }
        globalBoxCodeFilter.put(boxCode);
        log.debug("[BloomFilter] 添加箱码: {}", boxCode);
    }
    
    /**
     * 批量添加箱码到全局 Bloom Filter
     * 
     * @param boxCodes 要添加的箱码列表
     */
    public void putAllBoxCodes(Iterable<String> boxCodes) {
        int count = 0;
        for (String boxCode : boxCodes) {
            if (boxCode != null && !boxCode.isEmpty()) {
                globalBoxCodeFilter.put(boxCode);
                count++;
            }
        }
        log.info("[BloomFilter] 批量添加箱码: {} 个", count);
    }
    
    /**
     * 从布隆过滤器中删除箱码（标记为已删除）
     * 注意：布隆过滤器本身不支持删除操作，这里使用一个Set来跟踪已删除的码
     * 
     * @param boxCode 要删除的箱码
     */
    public void removeBoxCode(String boxCode) {
        if (boxCode == null || boxCode.isEmpty()) {
            return;
        }
        deletedBoxCodes.put(boxCode, true);
        log.debug("[BloomFilter] 删除箱码: {}", boxCode);
    }
    
    /**
     * 重建全局箱码 Bloom Filter
     * 清空当前数据，重新创建（通常在初始化时从数据库加载数据）
     */
    public synchronized void rebuildFilter() {
        log.info("[BloomFilter] 重建全局箱码 Bloom Filter");
        this.globalBoxCodeFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            EXPECTED_INSERTIONS,
            FALSE_POSITIVE_PROBABILITY
        );
        // 重建时清空已删除码的集合
        deletedBoxCodes.clear();
    }
}

