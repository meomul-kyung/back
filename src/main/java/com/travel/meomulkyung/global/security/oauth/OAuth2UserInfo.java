package com.travel.meomulkyung.global.security.oauth;

/**
 * 제공자별 사용자 정보를 동일한 형태로 꺼내기 위한 공통 인터페이스.
 * (Kakao/Google/Naver의 응답 JSON 구조가 서로 달라 구현체로 흡수한다.)
 */
public interface OAuth2UserInfo {

    /** 'kakao' | 'google' | 'naver' */
    String getProvider();

    /** 제공자 내부 사용자 고유 ID */
    String getProviderId();

    /** 이메일 (동의/제공 여부에 따라 null 가능) */
    String getEmail();

    /** 표시 이름/닉네임 */
    String getName();

    /** 프로필 이미지 URL (없으면 null) */
    String getProfileImageUrl();
}
