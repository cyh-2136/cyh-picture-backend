package com.cyh.cyhpicturebackend.manager;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class CacheManager {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private Cache<String, String> LOCAL_CACHE;

    /**
     * 刷新所有缓存
     */
    public void refreshAllCache() {
        // 清除本地缓存
        LOCAL_CACHE.invalidateAll();
        // 清除 Redis 缓存
        stringRedisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    /**
     * 刷新指定缓存
     * @param cacheName 缓存名称
     */
    public void refreshCache(String cacheName) {
        if ("all".equals(cacheName)) {
            refreshAllCache();
            return;
        }

        // 清除本地缓存中指定前缀的缓存
        LOCAL_CACHE.asMap().keySet().stream()
                .filter(key -> key.startsWith(cacheName))
                .forEach(LOCAL_CACHE::invalidate);

        // 清除 Redis 中指定前缀的缓存
        stringRedisTemplate.delete(stringRedisTemplate.keys(cacheName + "*"));
    }

    /**
     * 获取所有缓存名称
     */
    public List<String> getCacheNames() {
        // 从本地缓存中获取所有缓存键
        Set<String> cacheKeys = LOCAL_CACHE.asMap().keySet();
        
        // 提取缓存名称前缀
        Set<String> cacheNamePrefixes = new HashSet<>();
        for (String key : cacheKeys) {
            // 提取前缀（假设缓存键格式为 "prefix:key"）
            int colonIndex = key.indexOf(":");
            if (colonIndex > 0) {
                String prefix = key.substring(0, colonIndex);
                cacheNamePrefixes.add(prefix);
            } else {
                // 如果没有冒号，使用整个键作为前缀
                cacheNamePrefixes.add(key);
            }
        }
        
        // 从 Redis 中获取所有缓存键
        Set<String> redisKeys = stringRedisTemplate.keys("*");
        if (redisKeys != null) {
            for (String key : redisKeys) {
                // 提取前缀（假设缓存键格式为 "prefix:key"）
                int colonIndex = key.indexOf(":");
                if (colonIndex > 0) {
                    String prefix = key.substring(0, colonIndex);
                    cacheNamePrefixes.add(prefix);
                } else {
                    // 如果没有冒号，使用整个键作为前缀
                    cacheNamePrefixes.add(key);
                }
            }
        }
        
        // 转换为列表并返回
        return new ArrayList<>(cacheNamePrefixes);
    }
}