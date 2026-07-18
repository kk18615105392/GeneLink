package com.genelink.controller;

import com.genelink.web.ApiResponse;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private Cache<String, String> shortLinkCache;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/api/v1/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "UP");
        info.put("app", "GeneLink");
        info.put("techniques", new String[]{
                "gene-sharding",
                "multilevel-cache",
                "dcl",
                "segment-id",
                "shardingsphere-jdbc"
        });
        info.put("l1CacheEstimatedSize", shortLinkCache.estimatedSize());
        try {
            String pong = redisTemplate.getConnectionFactory() != null
                    ? redisTemplate.getConnectionFactory().getConnection().ping()
                    : "UNKNOWN";
            info.put("redis", pong);
        } catch (Exception e) {
            info.put("redis", "DOWN: " + e.getMessage());
        }
        info.put("shards", new String[]{"link_mapping_0", "link_mapping_1"});
        return ApiResponse.ok(info);
    }
}
