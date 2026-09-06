package com.travel.meomulkyung.user.repository;

import com.travel.meomulkyung.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 소셜 계정 식별키 (provider + providerId) 로 조회 */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    /** 닉네임 중복 확인 (본인 제외) */
    boolean existsByNicknameAndIdNot(String nickname, Long id);
}
