package com.travel.meomulkyung.user.controller;

import com.travel.meomulkyung.user.domain.User;
import com.travel.meomulkyung.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 회원 정보 조회/수정 엔드포인트.
 * Authorization: Bearer {JWT} 로 호출하면 현재 사용자 정보를 반환한다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /** 현재 로그인한 회원 정보 + 온보딩(닉네임 등록) 완료 여부 조회 */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "인증이 필요합니다."));
        }

        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(toResponse(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "사용자를 찾을 수 없습니다.")));
    }

    private Map<String, Object> toResponse(User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", user.getId());
        body.put("email", user.getEmail());
        body.put("name", user.getName());
        body.put("nickname", user.getNickname());
        body.put("profileImageUrl", user.getProfileImageUrl());
        body.put("provider", user.getProvider());
        body.put("role", user.getRole().name());
        body.put("onboardingCompleted", user.isOnboardingCompleted());
        return body;
    }
}
