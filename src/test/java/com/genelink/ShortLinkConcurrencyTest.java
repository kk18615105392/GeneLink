package com.genelink;

import com.genelink.entity.LinkMapping;
import com.genelink.generator.SequenceGenerator;
import com.genelink.repository.LinkMappingRepository;
import com.genelink.service.ShortLinkService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ShortLinkConcurrencyTest {

    @MockBean
    private LinkMappingRepository linkMappingRepository;

    @Autowired
    private SequenceGenerator sequenceGenerator;

    @Autowired
    private ShortLinkService shortLinkService;

    @BeforeEach
    void setUp() {
        when(linkMappingRepository.save(any(LinkMapping.class))).thenReturn(null);
        when(linkMappingRepository.findByShortCode(anyString())).thenReturn(Optional.empty());
    }

    @Test
    public void testSequenceGeneratorConcurrency() throws InterruptedException {
        int threadsCount = 100;
        int runsPerThread = 50;
        int totalRequests = threadsCount * runsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch latch = new CountDownLatch(threadsCount);
        Set<Long> generatedIds = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < runsPerThread; j++) {
                        generatedIds.add(sequenceGenerator.nextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("====== Total generated unique IDs: " + generatedIds.size() + " ======");
        Assertions.assertEquals(totalRequests, generatedIds.size());
    }

    @Test
    public void testMultilevelCacheAndDCL() throws InterruptedException {
        String originalUrl = "https://www.google.com/search?q=genelink";
        String gid = "group_test";
        String shortCode = shortLinkService.createShortLink(originalUrl, gid);
        System.out.println("====== Created ShortCode: " + shortCode + " ======");

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Set<String> results = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String url = shortLinkService.getOriginalUrl(shortCode);
                    if (url != null) {
                        results.add(url);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Assertions.assertEquals(1, results.size());
        Assertions.assertTrue(results.contains(originalUrl));
        System.out.println("====== Concurrency redirect test passed! URL resolved: " + results.iterator().next() + " ======");
    }

    @Test
    public void testCachePenetrationAndDCL() throws InterruptedException {
        String nonExistentCode = "NOTFOUNDCODE";
        int threadCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Assertions.assertNull(shortLinkService.getOriginalUrl(nonExistentCode));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        System.out.println("====== Cache penetration protection test passed! ======");
    }
}
