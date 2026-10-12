package com.kece.common.security;

import com.kece.common.exception.BusinessException;
import com.kece.common.response.ResultCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证与授权拦截器。
 *
 * <p>1) 尝试解析请求头 {@code Authorization: Bearer <token>} 到 {@link AuthContextHolder}； 2) 校验 {@link
 * RequireRole} 注解，未登录返回 4002，角色不足返回 4003。
 */
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        AuthContextHolder.clear();
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                AuthContextHolder.set(
                        jwtUtil.parseToAuthUser(header.substring(BEARER_PREFIX.length())));
            } catch (JwtException | IllegalArgumentException ignored) {
                // 令牌无效或过期：按未认证处理，由下方权限校验决定是否放行
            }
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return true;
        }

        AuthUser user = AuthContextHolder.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Role[] allowed = requireRole.value();
        if (allowed.length == 0) {
            return true;
        }
        for (Role role : allowed) {
            if (user.hasRole(role)) {
                return true;
            }
        }
        throw new BusinessException(ResultCode.FORBIDDEN);
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        AuthContextHolder.clear();
    }
}
