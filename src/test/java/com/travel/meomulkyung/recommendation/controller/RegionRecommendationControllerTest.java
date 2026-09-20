package com.travel.meomulkyung.recommendation.controller;

import com.travel.meomulkyung.global.security.SecurityConfig;
import com.travel.meomulkyung.global.security.jwt.JwtAuthenticationFilter;
import com.travel.meomulkyung.global.security.oauth.CustomOAuth2UserService;
import com.travel.meomulkyung.global.security.oauth.OAuth2FailureHandler;
import com.travel.meomulkyung.global.security.oauth.OAuth2SuccessHandler;
import com.travel.meomulkyung.recommendation.RegionTestFixture;
import com.travel.meomulkyung.recommendation.service.RegionRecommendationScoreCalculator;
import com.travel.meomulkyung.recommendation.service.RegionRecommendationService;
import com.travel.meomulkyung.region.repository.RegionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@WebMvcTest(RegionRecommendationController.class)
@Import({RegionRecommendationService.class, RegionRecommendationScoreCalculator.class, SecurityConfig.class,
        com.travel.meomulkyung.recommendation.RecommendationTestConfig.class})
class RegionRecommendationControllerTest {

    private static final String VALID_REQUEST = """
            {"preferenceTags":["NATURE","FOOD","WALKING"],"companionType":"FRIENDS","nights":2}
            """;

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

    @MockitoBean
    private RegionRepository regionRepository;

    @BeforeEach
    void continueMockedJwtFilterChain() throws Exception {
        when(regionRepository.findAllWithTagScores()).thenReturn(RegionTestFixture.regionsFromSeedProfiles());
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            if ("Bearer test-access-token".equals(((jakarta.servlet.http.HttpServletRequest) request).getHeader("Authorization"))) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(1L, null, List.of()));
            }
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    request,
                    (ServletResponse) invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    void recommendReturnsThreeRankedRegionsInStableOrder() throws Exception {
        String firstResponse = mockMvc.perform(post("/api/regions/recommendations")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(3))
                .andExpect(jsonPath("$.recommendations[0].rank").value(1))
                .andExpect(jsonPath("$.recommendations[1].rank").value(2))
                .andExpect(jsonPath("$.recommendations[2].rank").value(3))
                .andReturn().getResponse().getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/regions/recommendations")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(secondResponse).isEqualTo(firstResponse);
    }

    @Test
    void recommendWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/regions/recommendations").contentType(APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        assertBadRequest("{\"preferenceTags\":[],\"companionType\":\"FRIENDS\",\"nights\":2}");
        assertBadRequest("{\"preferenceTags\":[\"NATURE\",\"FOOD\",\"WALKING\",\"SEA\"],\"companionType\":\"FRIENDS\",\"nights\":2}");
        assertBadRequest("{\"preferenceTags\":[\"NATURE\",\"NATURE\"],\"companionType\":\"FRIENDS\",\"nights\":2}");
        assertBadRequest("{\"preferenceTags\":[\"UNKNOWN\"],\"companionType\":\"FRIENDS\",\"nights\":2}");
        assertBadRequest("{\"preferenceTags\":[\"NATURE\"],\"companionType\":\"UNKNOWN\",\"nights\":2}");
        assertBadRequest("{\"preferenceTags\":[\"NATURE\"],\"companionType\":\"FRIENDS\",\"nights\":0}");
        assertBadRequest("{\"preferenceTags\":[\"NATURE\"],\"companionType\":\"FRIENDS\",\"nights\":8}");
    }

    private void assertBadRequest(String content) throws Exception {
        mockMvc.perform(post("/api/regions/recommendations")
                        .header("Authorization", "Bearer test-access-token")
                        .contentType(APPLICATION_JSON).content(content))
                .andExpect(status().isBadRequest());
    }
}
