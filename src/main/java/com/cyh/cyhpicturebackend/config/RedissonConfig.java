package com.cyh.cyhpicturebackend.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${redisson.address}")
    private String address;

    @Value("${redisson.database}")
    private int database;

    @Value("${redisson.password}")
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        try {
            // 配置 Redis 连接
            config.useSingleServer()
                    .setAddress(address)
                    .setDatabase(database)
                    .setPassword(password);
            return Redisson.create(config);
        } catch (Exception e) {
            // 如果 Redis 连接失败，返回 null 或抛出明确的异常
            throw new RuntimeException("Redis 连接失败", e);
        }

    }
}