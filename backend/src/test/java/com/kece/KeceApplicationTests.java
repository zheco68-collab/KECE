package com.kece;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 上下文冒烟测试：通过 Testcontainers 启动 MySQL，验证 Flyway 迁移与应用上下文可正常加载， 使 CI 在无外部数据库的环境下也能运行测试。 */
@Testcontainers
@SpringBootTest
class KeceApplicationTests {

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("kece")
                    .withUsername("kece")
                    .withPassword("kece123")
                    .withUrlParam("allowPublicKeyRetrieval", "true")
                    .withUrlParam("useSSL", "false");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void contextLoads() {}
}
