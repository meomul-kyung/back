package com.travel.meomulkyung.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 소셜 로그인 사용자.
 *
 * <p>본 서비스는 소셜 로그인(Kakao/Google/Naver) 전용이므로 자체 비밀번호를 저장하지 않는다.
 * 계정 식별의 자연키는 (provider, providerId) 조합이다.
 * 이메일은 제공자/동의 여부에 따라 null일 수 있으므로 식별키로 쓰지 않는다.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_provider",
                columnNames = {"provider", "provider_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 제공자에서 내려준 이메일 (동의 안 하면 null 가능) */
    @Column(length = 320)
    private String email;

    /** 닉네임/이름 */
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String profileImageUrl;

    /** 사용자 지정 닉네임 (온보딩 시 등록, 서비스 내 표시명). 등록 전엔 null. */
    @Column(unique = true, length = 30)
    private String nickname;

    /** 'kakao' | 'google' | 'naver' */
    @Column(nullable = false, length = 20)
    private String provider;

    /** 제공자 내부 사용자 고유 ID */
    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    private User(String email, String name, String profileImageUrl,
                 String provider, String providerId, Role role) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

    /** 재로그인 시 최신 프로필로 갱신 */
    public void updateProfile(String name, String profileImageUrl, String email) {
        if (name != null) {
            this.name = name;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        if (email != null) {
            this.email = email;
        }
    }

    /** 온보딩/개인정보 수정 시 닉네임 등록·변경 */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 온보딩 완료 여부는 닉네임 등록 여부로 파생한다(별도 플래그 없음). */
    public boolean isOnboardingCompleted() {
        return this.nickname != null;
    }
}
