package com.travel.meomulkyung.global.security.oauth;

import com.travel.meomulkyung.user.domain.Role;
import com.travel.meomulkyung.user.domain.User;
import com.travel.meomulkyung.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 제공자에서 받은 사용자 정보를 우리 User로 저장/갱신하고 CustomOAuth2User로 감싸 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // NPE 디버깅용: 제공자가 실제로 내려준 원본 구조 확인
        log.debug("OAuth2 user attributes received for registrationId={}", registrationId);

        OAuth2UserInfo info = OAuth2UserInfoFactory.of(registrationId, attributes);
        if (info.getProviderId() == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("no_provider_id"), "제공자에서 사용자 ID를 가져올 수 없습니다.");
        }

        User user = saveOrUpdate(info);
        return new CustomOAuth2User(user, attributes);
    }

    private User saveOrUpdate(OAuth2UserInfo info) {
        return userRepository.findByProviderAndProviderId(info.getProvider(), info.getProviderId())
                .map(existing -> {
                    existing.updateProfile(info.getName(), info.getProfileImageUrl(), info.getEmail());
                    return existing;
                })
                .orElseGet(() -> {
                    String name = info.getName() != null
                            ? info.getName()
                            : info.getProvider() + "_" + info.getProviderId();
                    log.info("[OAuth2] 신규 가입 - provider={}, email={}", info.getProvider(), info.getEmail());
                    return userRepository.save(User.builder()
                            .email(info.getEmail())
                            .name(name)
                            .profileImageUrl(info.getProfileImageUrl())
                            .provider(info.getProvider())
                            .providerId(info.getProviderId())
                            .role(Role.USER)
                            .build());
                });
    }
}
