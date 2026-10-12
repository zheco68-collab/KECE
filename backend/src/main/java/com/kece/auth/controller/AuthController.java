package com.kece.auth.controller;

import com.kece.auth.dto.AuthResponse;
import com.kece.auth.dto.LoginRequest;
import com.kece.auth.dto.RefreshRequest;
import com.kece.auth.dto.RegisterRequest;
import com.kece.auth.dto.UserVO;
import com.kece.auth.service.AuthService;
import com.kece.common.response.ApiResponse;
import com.kece.common.security.AuthContextHolder;
import com.kece.common.security.AuthUser;
import com.kece.common.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证接口。 */
@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "登出")
    @RequireRole
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // JWT 无状态：登出由前端清除令牌完成；此处预留扩展点（黑名单/吊销）。
        return ApiResponse.success();
    }

    @Operation(summary = "当前用户信息")
    @RequireRole
    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        AuthUser user = AuthContextHolder.get();
        return ApiResponse.success(authService.currentUser(user));
    }
}
