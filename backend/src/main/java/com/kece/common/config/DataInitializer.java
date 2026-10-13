package com.kece.common.config;

import com.kece.user.entity.User;
import com.kece.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 种子数据初始化：确保系统存在默认管理员/出题人账号，幂等（按用户名查重）。
 *
 * <p>受 {@code kece.demo-data.enabled} 控制（默认关、仅 dev profile 开）：生产环境不播种默认口令账号。 与客观题示例播种（{@link
 * ObjectiveQuestionSeeder}）通过 {@code @Order} 固定先后顺序，避免时序竞态。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${kece.demo-data.enabled:false}")
    private boolean demoDataEnabled;

    /** 先于客观题播种执行，确保 teacher 账号已存在。 */
    @Order(1)
    @Bean
    public ApplicationRunner seedData() {
        return args -> {
            if (!demoDataEnabled) {
                log.info("演示数据开关已关闭（kece.demo-data.enabled=false），跳过种子账号播种");
                return;
            }
            ensureUser("admin", "admin123", "admin@kece.local", "ADMIN");
            ensureUser("teacher", "teacher123", "teacher@kece.local", "TEACHER");
        };
    }

    private User ensureUser(String username, String rawPassword, String email, String role) {
        User existing = userMapper.findByUsername(username);
        if (existing != null) {
            return existing;
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setRoles(role);
        user.setStatus(1);
        userMapper.insert(user);
        log.info("已创建种子用户: {}（角色 {}）", username, role);
        return user;
    }
}
