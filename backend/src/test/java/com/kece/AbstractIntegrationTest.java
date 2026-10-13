package com.kece;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

/**
 * 集成测试基类。
 * <p>
 * 默认使用 Testcontainers 启动 MySQL/Redis；当系统属性 {@code test.local=true} 时，
 * 回退到本机 MySQL(kece/kece123@localhost:3306) 与 Redis(localhost:6379)，
 * 用于 Docker/Testcontainers 不可用的环境。
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final boolean USE_LOCAL = Boolean.getBoolean("test.local");

    private static final MySQLContainer<?> MYSQL;
    private static final GenericContainer<?> REDIS;

    static {
        if (USE_LOCAL) {
            MYSQL = null;
            REDIS = null;
        } else {
            MYSQL =
                    new MySQLContainer<>("mysql:8.0")
                            .withDatabaseName("kece")
                            .withUsername("kece")
                            .withPassword("kece123")
                            .withUrlParam("allowPublicKeyRetrieval", "true")
                            .withUrlParam("useSSL", "false");
            REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
            MYSQL.start();
            REDIS.start();
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
            registry.add("spring.data.redis.host", () -> "localhost");
            registry.add("spring.data.redis.port", () -> 6379);
        } else {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
            registry.add("spring.data.redis.host", REDIS::getHost);
            registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        }
    }

    @Autowired protected MockMvc mockMvc;

    @Autowired protected ObjectMapper objectMapper;

    /** 登录并返回访问令牌。 */
    protected String loginToken(String username, String password) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of(
                                                                "username",
                                                                username,
                                                                "password",
                                                                password))))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode root = body(result);
        assertThat(root.path("code").asInt()).isEqualTo(200);
        return root.path("data").path("token").asText();
    }

    /** 解析响应体为 JsonNode。 */
    protected JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** 解析响应体中 {@code data} 节点。 */
    protected JsonNode data(MvcResult result) throws Exception {
        return body(result).path("data");
    }

    /** 解析响应体中业务状态码 {@code code}。 */
    protected int code(MvcResult result) throws Exception {
        return body(result).path("code").asInt();
    }
}
