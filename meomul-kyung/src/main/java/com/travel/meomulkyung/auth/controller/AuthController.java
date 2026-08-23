package com.travel.meomulkyung.auth.controller;

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
 * 로그인 검증용 최소 엔드포인트.
 * Authorization: Bearer {JWT} 로 호출하면 현재 사용자 정보를 반환한다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

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
        body.put("profileImageUrl", user.getProfileImageUrl());
        body.put("provider", user.getProvider());
        body.put("role", user.getRole().name());
        return body;
    }
}
