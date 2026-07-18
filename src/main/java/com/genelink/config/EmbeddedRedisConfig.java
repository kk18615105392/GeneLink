package com.genelink.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

/**
 * 本地零依赖启动用。CI / 已有外部 Redis 时通过
 * genelink.embedded-redis.enabled=false 关闭。
 */
@Configuration
@ConditionalOnProperty(name = "genelink.embedded-redis.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddedRedisConfig {

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        try {
            redisServer = new RedisServer(redisPort);
            redisServer.start();
            System.out.println("====== Embedded Redis started on port: " + redisPort + " ======");
        } catch (Exception e) {
            System.err.println("====== Embedded Redis failed to start (port might be in use): " + e.getMessage() + " ======");
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            try {
                redisServer.stop();
                System.out.println("====== Embedded Redis stopped ======");
            } catch (Exception e) {
                System.err.println("====== Failed to stop Embedded Redis: " + e.getMessage() + " ======");
            }
        }
    }
}
