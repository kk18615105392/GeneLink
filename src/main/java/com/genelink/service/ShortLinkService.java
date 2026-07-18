package com.genelink.service;

import com.genelink.entity.LinkMapping;
import com.genelink.generator.SequenceGenerator;
import com.genelink.repository.LinkMappingRepository;
import com.genelink.util.Base62Utils;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class ShortLinkService {

    private static final String REDIS_CACHE_PREFIX = "genelink:cache:";
    private static final String NULL_PLACEHOLDER = "-";
    private static final String CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Autowired
    private SequenceGenerator sequenceGenerator;

    @Autowired
    private LinkMappingRepository linkMappingRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private Cache<String, String> shortLinkCache;

    private final Object lock = new Object();

    /**
     * 创建短链：号段发号 → Base62 → 基因注入 → 落库 → 多级缓存预热
     */
    public String createShortLink(String originalUrl, String gid) {
        long sequenceId = sequenceGenerator.nextId();
        String base62Code = Base62Utils.encode(sequenceId);

        int geneIndex = Math.abs(gid.hashCode()) % 62;
        char geneChar = CHARACTERS.charAt(geneIndex);
        String shortCode = base62Code + geneChar;

        LinkMapping linkMapping = LinkMapping.builder()
                .shortCode(shortCode)
                .originalUrl(originalUrl)
                .gid(gid)
                .build();
        linkMappingRepository.save(linkMapping);
        writeToCache(shortCode, originalUrl);
        return shortCode;
    }

    /**
     * 解析短链：Caffeine → Redis → DCL → DB → 回写 / 空值防穿透
     */
    public String getOriginalUrl(String shortCode) {
        String originalUrl = shortLinkCache.getIfPresent(shortCode);
        if (originalUrl != null) {
            return NULL_PLACEHOLDER.equals(originalUrl) ? null : originalUrl;
        }

        String cacheKey = REDIS_CACHE_PREFIX + shortCode;
        originalUrl = redisTemplate.opsForValue().get(cacheKey);
        if (originalUrl != null) {
            shortLinkCache.put(shortCode, originalUrl);
            return NULL_PLACEHOLDER.equals(originalUrl) ? null : originalUrl;
        }

        synchronized (lock) {
            originalUrl = shortLinkCache.getIfPresent(shortCode);
            if (originalUrl != null) {
                return NULL_PLACEHOLDER.equals(originalUrl) ? null : originalUrl;
            }

            originalUrl = redisTemplate.opsForValue().get(cacheKey);
            if (originalUrl != null) {
                shortLinkCache.put(shortCode, originalUrl);
                return NULL_PLACEHOLDER.equals(originalUrl) ? null : originalUrl;
            }

            System.out.println("====== Cache Miss! Querying Database for ShortCode: " + shortCode + " ======");
            Optional<LinkMapping> mappingOpt = linkMappingRepository.findByShortCode(shortCode);
            if (mappingOpt.isPresent()) {
                originalUrl = mappingOpt.get().getOriginalUrl();
                int expireMinutes = 10 + ThreadLocalRandom.current().nextInt(6);
                redisTemplate.opsForValue().set(cacheKey, originalUrl, expireMinutes, TimeUnit.MINUTES);
                shortLinkCache.put(shortCode, originalUrl);
            } else {
                redisTemplate.opsForValue().set(cacheKey, NULL_PLACEHOLDER, 30, TimeUnit.SECONDS);
                shortLinkCache.put(shortCode, NULL_PLACEHOLDER);
                originalUrl = null;
            }
        }
        return originalUrl;
    }

    public Optional<LinkMapping> findByShortCode(String shortCode) {
        return linkMappingRepository.findByShortCode(shortCode);
    }

    private void writeToCache(String shortCode, String originalUrl) {
        String cacheKey = REDIS_CACHE_PREFIX + shortCode;
        int expireMinutes = 10 + ThreadLocalRandom.current().nextInt(6);
        redisTemplate.opsForValue().set(cacheKey, originalUrl, expireMinutes, TimeUnit.MINUTES);
        shortLinkCache.put(shortCode, originalUrl);
    }
}
