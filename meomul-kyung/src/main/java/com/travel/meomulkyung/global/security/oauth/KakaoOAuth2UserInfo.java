package com.travel.meomulkyung.global.security.oauth;

import java.util.Map;

/**
 * 카카오 응답 구조:
 * {
 *   "id": 12345,
 *   "kakao_account": {
 *     "email": "user@kakao.com",
 *     "profile": { "nickname": "홍길동", "profile_image_url": "..." }
 *   },
 *   "properties": { "nickname": "홍길동", "profile_image": "..." }
 * }
 */
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> kakaoAccount() {
        Object account = attributes.get("kakao_account");
        return account instanceof Map ? (Map<String, Object>) account : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> profile() {
        Map<String, Object> account = kakaoAccount();
        if (account == null) {
            return null;
        }
        Object profile = account.get("profile");
        return profile instanceof Map ? (Map<String, Object>) profile : null;
    }

    @Override
    public String getEmail() {
        Map<String, Object> account = kakaoAccount();
        return account == null ? null : (String) account.get("email");
    }

    @Override
    public String getName() {
        Map<String, Object> profile = profile();
        return profile == null ? null : (String) profile.get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        Map<String, Object> profile = profile();
        return profile == null ? null : (String) profile.get("profile_image_url");
    }
}
