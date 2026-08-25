package com.travel.meomulkyung.user.service;

import com.travel.meomulkyung.user.domain.User;
import com.travel.meomulkyung.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 회원(닉네임/온보딩/개인정보) 관련 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /** 닉네임 규칙: 2~20자, 한글/영문/숫자/밑줄(_)만 허용 */
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9_]{2,20}$");

    /** 닉네임 사용 가능 여부 (형식 검증 후 본인 제외 중복 확인) */
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(Long userId, String nickname) {
        requireAuth(userId);
        validateNicknameFormat(nickname);
        return !userRepository.existsByNicknameAndIdNot(nickname, userId);
    }

    /** 최초 닉네임 등록(온보딩). 이미 등록된 회원은 거부. */
    @Transactional
    public User registerNickname(Long userId, String nickname) {
        User user = getUser(userId);
        if (user.isOnboardingCompleted()) {
            throw new IllegalStateException("이미 닉네임이 등록되어 있습니다. 개인정보 수정을 이용하세요.");
        }
        validateNicknameFormat(nickname);
        ensureNicknameNotTaken(userId, nickname);
        user.updateNickname(nickname);
        return user;
    }

    /** 이름·닉네임 수정 (전달된 값만 반영). */
    @Transactional
    public User updateProfile(Long userId, String name, String nickname) {
        User user = getUser(userId);
        if (nickname != null) {
            validateNicknameFormat(nickname);
            ensureNicknameNotTaken(userId, nickname);
            user.updateNickname(nickname);
        }
        if (name != null) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("이름은 비어 있을 수 없습니다.");
            }
            // updateProfile은 null이 아닌 값만 반영하므로 이름만 갱신된다.
            user.updateProfile(name, null, null);
        }
        return user;
    }

    private User getUser(Long userId) {
        requireAuth(userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private void requireAuth(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }
    }

    private void validateNicknameFormat(String nickname) {
        if (nickname == null || !NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new IllegalArgumentException("닉네임은 2~20자의 한글, 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.");
        }
    }

    private void ensureNicknameNotTaken(Long userId, String nickname) {
        if (userRepository.existsByNicknameAndIdNot(nickname, userId)) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }
    }
}
