package com.travel.meomulkyung.global.security.oauth;

import com.travel.meomulkyung.global.security.jwt.JwtTokenProvider;
import com.travel.meomulkyung.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * 소셜 로그인 성공 → JWT 발급 → 프론트 콜백 주소로 토큰과 함께 리다이렉트.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        log.info("[OAuth2 로그인 성공] userId={}, provider={}, email={}",
                user.getId(), user.getProvider(), user.getEmail());
        // 개발 편의용 토큰 로깅 — 운영 배포 전 반드시 제거/레벨조정
        log.debug("[OAuth2 발급 토큰(dev)] {}", accessToken);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
