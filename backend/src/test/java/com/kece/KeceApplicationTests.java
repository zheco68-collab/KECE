package com.kece;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * 上下文冒烟测试：验证 Flyway 迁移与应用上下文可正常加载。 默认走 Testcontainers MySQL；系统属性 {@code test.local=true} 时回退到本机
 * MySQL。
 */
@SpringBootTest
class KeceApplicationTests {

    private static final boolean USE_LOCAL = Boolean.getBoolean("test.local");

    private static final MySQLContainer<?> MYSQL;

    static {
        if (USE_LOCAL) {
            MYSQL = null;
        } else {
            MYSQL =
                    new MySQLContainer<>("mysql:8.0")
                            .withDatabaseName("kece")
                            .withUsername("kece")
                            .withPassword("kece123")
                            .withUrlParam("allowPublicKeyRetrieval", "true")
                            .withUrlParam("useSSL", "false");
            MYSQL.start();
        }
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        if (USE_LOCAL) {
            registry.add(
                    "spring.datasource.url",
                    () ->
                            "jdbc:mysql://localhost:3306/kece?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci");
            registry.add("spring.datasource.username", () -> "kece");
            registry.add("spring.datasource.password", () -> "kece123");
        } else {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
        }
    }

    @Test
    void contextLoads() {}
}
