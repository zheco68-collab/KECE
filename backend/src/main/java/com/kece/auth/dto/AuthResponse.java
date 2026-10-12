package com.kece.auth.dto;

import lombok.Builder;
import lombok.Data;

/** 认证响应：访问令牌 + 刷新令牌 + 用户信息。 */
@Data
@Builder
public class AuthResponse {

    private String token;
    private String refreshToken;
    private UserVO user;
}
