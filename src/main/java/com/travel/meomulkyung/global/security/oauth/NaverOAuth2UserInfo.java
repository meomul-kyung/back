package com.travel.meomulkyung.global.security.oauth;

import java.util.Map;

/**
 * 네이버 응답 구조 (사용자 정보가 response 안에 감싸여 옴):
 * {
 *   "resultcode": "00", "message": "success",
 *   "response": { "id": "...", "email": "...", "name": "...", "profile_image": "..." }
 * }
 */
public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> response;

    @SuppressWarnings("unchecked")
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        Object res = attributes.get("response");
        this.response = res instanceof Map ? (Map<String, Object>) res : Map.of();
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getProviderId() {
        return (String) response.get("id");
    }

    @Override
    public String getEmail() {
        return (String) response.get("email");
    }

    @Override
    public String getName() {
        return (String) response.get("name");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) response.get("profile_image");
    }
}
