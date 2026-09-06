package com.travel.meomulkyung.user.controller;

import com.travel.meomulkyung.user.domain.User;
import com.travel.meomulkyung.user.dto.OnboardingRequest;
import com.travel.meomulkyung.user.dto.ProfileUpdateRequest;
import com.travel.meomulkyung.user.repository.UserRepository;
import com.travel.meomulkyung.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 회원 정보 조회/수정 엔드포인트.
 * 모든 엔드포인트는 Authorization: Bearer {JWT} 인증이 필요하다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

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

    /** 닉네임 사용 가능 확인 */
    @GetMapping("/nickname-availability")
    public ResponseEntity<?> checkNicknameAvailability(@AuthenticationPrincipal Long userId,
                                                       @RequestParam String nickname) {
        boolean available = userService.isNicknameAvailable(userId, nickname);
        Map<String, Object> body = new HashMap<>();
        body.put("nickname", nickname);
        body.put("available", available);
        return ResponseEntity.ok(body);
    }

    /** 최초 닉네임 등록 (온보딩) */
    @PatchMapping("/me/onboarding")
    public ResponseEntity<?> onboarding(@AuthenticationPrincipal Long userId,
                                        @RequestBody OnboardingRequest request) {
        User user = userService.registerNickname(userId, request.nickname());
        return ResponseEntity.ok(toResponse(user));
    }

    /** 개인정보(이름·닉네임) 수정 */
    @PatchMapping("/me")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal Long userId,
                                           @RequestBody ProfileUpdateRequest request) {
        User user = userService.updateProfile(userId, request.name(), request.nickname());
        return ResponseEntity.ok(toResponse(user));
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
