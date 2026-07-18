package com.genelink;

import com.genelink.config.DatabaseInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * GeneLink — 基因路由短链平台
 */
@SpringBootApplication
@EnableCaching
public class GeneLinkApplication {

    public static void main(String[] args) {
        // 必须在 Spring / ShardingSphere 初始化前建好物理分片表
        DatabaseInitializer.createPhysicalTables();
        SpringApplication.run(GeneLinkApplication.class, args);
    }
}

