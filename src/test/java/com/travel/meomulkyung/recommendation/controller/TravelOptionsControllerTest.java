package com.travel.meomulkyung.recommendation.controller;

import com.travel.meomulkyung.global.security.SecurityConfig;
import com.travel.meomulkyung.global.security.jwt.JwtAuthenticationFilter;
import com.travel.meomulkyung.global.security.oauth.CustomOAuth2UserService;
import com.travel.meomulkyung.global.security.oauth.OAuth2FailureHandler;
import com.travel.meomulkyung.global.security.oauth.OAuth2SuccessHandler;
import com.travel.meomulkyung.recommendation.service.TravelOptionsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@WebMvcTest(TravelOptionsController.class)
@Import({TravelOptionsService.class, SecurityConfig.class})
class TravelOptionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    private OAuth2FailureHandler oAuth2FailureHandler;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void continueMockedJwtFilterChain() throws Exception {
        doAnswer(invocation -> {
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    (ServletRequest) invocation.getArgument(0),
                    (ServletResponse) invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    void getTravelOptionsWithoutAccessTokenReturnsExpectedOptions() throws Exception {
        mockMvc.perform(get("/api/travel-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferenceTags.length()").value(9))
                .andExpect(jsonPath("$.preferenceTags[0].code").value("HANOK_CONFUCIANISM"))
                .andExpect(jsonPath("$.preferenceTags[0].label").value("한옥·유교"))
                .andExpect(jsonPath("$.preferenceTags[1].code").value("NATURE"))
                .andExpect(jsonPath("$.preferenceTags[1].label").value("자연"))
                .andExpect(jsonPath("$.preferenceTags[2].code").value("SEA"))
                .andExpect(jsonPath("$.preferenceTags[2].label").value("바다"))
                .andExpect(jsonPath("$.preferenceTags[3].code").value("WALKING"))
                .andExpect(jsonPath("$.preferenceTags[3].label").value("걷기"))
                .andExpect(jsonPath("$.preferenceTags[4].code").value("HEALING"))
                .andExpect(jsonPath("$.preferenceTags[4].label").value("힐링"))
                .andExpect(jsonPath("$.preferenceTags[5].code").value("FOOD"))
                .andExpect(jsonPath("$.preferenceTags[5].label").value("음식"))
                .andExpect(jsonPath("$.preferenceTags[6].code").value("BICYCLE"))
                .andExpect(jsonPath("$.preferenceTags[6].label").value("자전거"))
                .andExpect(jsonPath("$.preferenceTags[7].code").value("HISTORY"))
                .andExpect(jsonPath("$.preferenceTags[7].label").value("역사"))
                .andExpect(jsonPath("$.preferenceTags[8].code").value("NIGHT_SKY"))
                .andExpect(jsonPath("$.preferenceTags[8].label").value("별·밤하늘"))
                .andExpect(jsonPath("$.preferenceSelection.minimum").value(1))
                .andExpect(jsonPath("$.preferenceSelection.maximum").value(3))
                .andExpect(jsonPath("$.companionTypes.length()").value(4))
                .andExpect(jsonPath("$.companionTypes[0].code").value("SOLO"))
                .andExpect(jsonPath("$.companionTypes[0].label").value("혼자"))
                .andExpect(jsonPath("$.companionTypes[1].code").value("COUPLE"))
                .andExpect(jsonPath("$.companionTypes[1].label").value("연인"))
                .andExpect(jsonPath("$.companionTypes[2].code").value("FRIENDS"))
                .andExpect(jsonPath("$.companionTypes[2].label").value("친구"))
                .andExpect(jsonPath("$.companionTypes[3].code").value("FAMILY"))
                .andExpect(jsonPath("$.companionTypes[3].label").value("가족"))
                .andExpect(jsonPath("$.stayDuration.minimumNights").value(1))
                .andExpect(jsonPath("$.stayDuration.maximumNights").value(7));
    }
}
