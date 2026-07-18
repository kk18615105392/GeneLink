package com.genelink.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 在容器 refresh（含 DataSource/ShardingSphere）之前创建物理分片表。
 * 覆盖 spring-boot:run 与 @SpringBootTest 两条启动路径。
 */
public class H2TableContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        DatabaseInitializer.createPhysicalTables();
    }
}
