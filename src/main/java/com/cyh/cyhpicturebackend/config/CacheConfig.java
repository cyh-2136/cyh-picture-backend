package com.cyh.cyhpicturebackend.config;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.concurrent.TimeUnit;


@Configuration
public class CacheConfig {

    @Value("${cache.local.initial-capacity:1024}")
    private int initialCapacity;

    @Value("${cache.local.maximum-size:10000}")
    private long maximumSize;

    @Value("${cache.local.expire-time:5}")
    private long expireTime;

    @Bean
    public Cache<String, String> localCache() {
        return Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .maximumSize(maximumSize)
                .expireAfterWrite(expireTime, TimeUnit.MINUTES)
                .build();
    }
}
