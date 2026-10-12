package com.kece.common.config;

import com.kece.user.entity.User;
import com.kece.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 种子数据初始化：确保系统存在默认管理员/出题人账号，幂等（按用户名查重）。
 *
 * <p>题目与测试用例由后台/MySQL 维护，不在此播种。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner seedData() {
        return args -> {
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
