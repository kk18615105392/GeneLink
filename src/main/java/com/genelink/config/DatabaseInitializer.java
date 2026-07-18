package com.genelink.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 必须在 ShardingSphere DataSource 初始化之前创建物理表，
 * 否则 5.4.x 启动时加载不到分片元数据，运行期会报逻辑表 link_mapping 不存在。
 */
@Component
@Order(0)
public class DatabaseInitializer implements ApplicationRunner {

    /** 必须与 sharding.yaml 中 ds_0.jdbcUrl 完全一致 */
    public static final String JDBC_URL =
            "jdbc:h2:mem:genelink;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MYSQL";

    public static void createPhysicalTables() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("H2 driver not found", e);
        }

        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             Statement stmt = conn.createStatement()) {

            for (String table : new String[]{"link_mapping_0", "link_mapping_1"}) {
                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS " + table + " (" +
                                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                                "short_code VARCHAR(16) NOT NULL, " +
                                "original_url VARCHAR(1024) NOT NULL, " +
                                "gid VARCHAR(32) NOT NULL, " +
                                "create_time TIMESTAMP NOT NULL" +
                                ")"
                );
                stmt.execute(
                        "CREATE UNIQUE INDEX IF NOT EXISTS idx_" + table + "_short_code ON " +
                                table + "(short_code)"
                );
            }
            System.out.println("====== [DatabaseInitializer] physical tables ready ======");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init sharding tables", e);
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        verifyTables();
    }

    private static void verifyTables() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, "sa", "");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                             "WHERE TABLE_NAME IN ('link_mapping_0', 'link_mapping_1')")) {
            if (rs.next() && rs.getInt(1) < 2) {
                throw new IllegalStateException("Expected 2 physical tables, found " + rs.getInt(1));
            }
            System.out.println("====== [DatabaseInitializer] verified link_mapping_0/1 ======");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify sharding tables", e);
        }
    }
}
