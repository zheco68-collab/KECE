package com.kece.common.security;

/** 系统角色。游客为未认证状态，不属于本枚举；认证用户至少具备 USER。 */
public enum Role {
    USER,
    TEACHER,
    ADMIN
}
