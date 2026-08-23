package com.travel.meomulkyung.user.domain;

/**
 * 사용자 권한.
 * 소셜 로그인으로 가입하는 일반 사용자는 USER, 운영자는 ADMIN.
 */
public enum Role {
    USER,
    ADMIN
}
