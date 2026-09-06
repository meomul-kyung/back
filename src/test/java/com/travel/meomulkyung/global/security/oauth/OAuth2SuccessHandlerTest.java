package com.travel.meomulkyung.global.security.oauth;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.travel.meomulkyung.global.security.jwt.JwtTokenProvider;
import com.travel.meomulkyung.user.domain.Role;
import com.travel.meomulkyung.user.domain.User;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2SuccessHandlerTest {

    @Test
    void issuesTokenWithoutWritingItsValueToLogsAndPreservesRedirectContract() throws Exception {
        String issuedToken = "test-access-token-that-must-not-appear-in-logs";
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        when(jwtTokenProvider.createAccessToken(1L, "user@example.com", "USER"))
                .thenReturn(issuedToken);

        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(jwtTokenProvider);
        ReflectionTestUtils.setField(handler, "redirectUri", "https://frontend.example.test/oauth/callback");

        User user = User.builder()
                .email("user@example.com")
                .name("Test User")
                .provider("test")
                .providerId("provider-user-1")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        CustomOAuth2User principal = new CustomOAuth2User(user, Map.of());
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        Logger logger = (Logger) LoggerFactory.getLogger(OAuth2SuccessHandler.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        try {
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

            assertThat(response.getRedirectedUrl())
                    .isEqualTo("https://frontend.example.test/oauth/callback?accessToken=" + issuedToken);
            assertThat(logs.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .noneMatch(message -> message.contains(issuedToken));
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }
}
