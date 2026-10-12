package com.kece.common.security;

/** 基于 ThreadLocal 的认证上下文，供 service 层读取当前用户身份。 */
public final class AuthContextHolder {

    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private AuthContextHolder() {}

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    public static AuthUser get() {
        return HOLDER.get();
    }

    public static Long currentUserId() {
        AuthUser user = HOLDER.get();
        return user == null ? null : user.getId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
