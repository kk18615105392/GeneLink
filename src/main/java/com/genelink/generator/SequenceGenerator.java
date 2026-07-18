package com.genelink.generator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis 号段模式 ID 生成器：
 * 每次 INCRBY 批量申请号段，本地 CAS 分配，降低 Redis 压力。
 */
@Component
public class SequenceGenerator {

    private static final String REDIS_SEQUENCE_KEY = "genelink:sequence";
    private static final int STEP = 1000;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final AtomicLong currentId = new AtomicLong(0);
    private volatile long maxId = 0;

    public long nextId() {
        while (true) {
            long current = currentId.get();
            if (current < maxId) {
                if (currentId.compareAndSet(current, current + 1)) {
                    return current + 1;
                }
            } else {
                synchronized (this) {
                    if (currentId.get() >= maxId) {
                        loadNextSegment();
                    }
                }
            }
        }
    }

    private void loadNextSegment() {
        Long nextMaxId = redisTemplate.opsForValue().increment(REDIS_SEQUENCE_KEY, STEP);
        if (nextMaxId == null) {
            throw new RuntimeException("Failed to load sequence from Redis");
        }
        this.maxId = nextMaxId;
        this.currentId.set(nextMaxId - STEP);
        System.out.println("====== Loaded new segment: [" + (nextMaxId - STEP + 1) + ", " + nextMaxId + "] ======");
    }
}
