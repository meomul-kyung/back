package com.travel.meomulkyung.global.security.oauth;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

/**
 * registrationId(kakao/google/naver)에 맞는 OAuth2UserInfo 구현체를 생성.
 */
public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {
    }

    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "naver" -> new NaverOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "지원하지 않는 소셜 로그인 제공자입니다: " + registrationId
            );
        };
    }
}
