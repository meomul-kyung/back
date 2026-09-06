package com.travel.meomulkyung.mypage.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.meomulkyung.global.security.SecurityConfig;
import com.travel.meomulkyung.global.security.jwt.JwtAuthenticationFilter;
import com.travel.meomulkyung.global.security.jwt.JwtTokenProvider;
import com.travel.meomulkyung.global.security.oauth.CustomOAuth2UserService;
import com.travel.meomulkyung.global.security.oauth.OAuth2FailureHandler;
import com.travel.meomulkyung.global.security.oauth.OAuth2SuccessHandler;
import com.travel.meomulkyung.itinerary.ItineraryTestConfig;
import com.travel.meomulkyung.itinerary.ItineraryTestFixture;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.repository.RegionRepository;
import com.travel.meomulkyung.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 마이페이지 5개 엔드포인트 통합 테스트.
 *
 * <p>TourAPI·기상·축제는 {@link ItineraryTestConfig}의 가짜 구현체로 대체되므로 외부 인증키 없이 돌아간다.
 * 일정 생성 → 저장 → 완료 등록까지 실제 API로 수행한 뒤 마이페이지 조회 결과를 검증한다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mypage-controller;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "logging.level.org.springframework=warn",
        "logging.level.org.hibernate=warn"
})
@AutoConfigureMockMvc
@Import({ItineraryTestConfig.class, SecurityConfig.class})
class MyPageControllerTest {

    private static final LocalDate START_DATE = LocalDate.now(ItineraryTestConfig.FIXED_CLOCK).plusDays(11);
    private static final long DAILY_SPENDING = 66_000L;
    private static final String POLICY_VERSION = "2025-KNTS-v1";
    private static final long REGION_A = 1L;
    private static final long REGION_B = 2L;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired RegionRepository regions;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ItineraryTestConfig.FakeFestivalProvider festivals;

    @MockitoBean CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockitoBean OAuth2FailureHandler oAuth2FailureHandler;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() throws Exception {
        ensureRegion(REGION_A, "테스트 지역 A");
        ensureRegion(REGION_B, "테스트 지역 B");
        festivals.setFestivals(List.of());
        stubAuthenticationFilter();
    }

    @Test
    @DisplayName("완료 여행 목록은 본인 것만 내려주고 regionId로 필터링된다")
    void completedTripsReturnsOwnTripsAndFiltersByRegion() throws Exception {
        Long userId = user();
        complete(userId, create(userId, REGION_A), 50, 2, 320_000);
        complete(userId, create(userId, REGION_B), 24, 1, 1_000);

        Long otherUserId = user();
        complete(otherUserId, create(otherUserId, REGION_A), 24, 1, 5_000);

        mockMvc.perform(get("/api/users/me/completed-trips").header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.completedTrips.length()").value(2));

        mockMvc.perform(get("/api/users/me/completed-trips").param("regionId", String.valueOf(REGION_A))
                        .header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.completedTrips[0].region.regionId").value((int) REGION_A))
                .andExpect(jsonPath("$.completedTrips[0].stayHours").value(50))
                .andExpect(jsonPath("$.completedTrips[0].partySize").value(2))
                .andExpect(jsonPath("$.completedTrips[0].estimatedSpending").value(264_000))
                .andExpect(jsonPath("$.completedTrips[0].populationContributionDays").value(2));
    }

    @Test
    @DisplayName("완료 여행이 없는 지역을 조회해도 에러가 아니라 빈 목록이다")
    void completedTripsReturnsEmptyListForRegionWithoutTrips() throws Exception {
        Long userId = user();
        complete(userId, create(userId, REGION_A), 24, 1, 0);

        mockMvc.perform(get("/api/users/me/completed-trips").param("regionId", String.valueOf(REGION_B))
                        .header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.completedTrips").isArray())
                .andExpect(jsonPath("$.completedTrips.length()").value(0));
    }

    @Test
    @DisplayName("상세 조회는 완료 시점에 저장된 기여도 스냅샷과 스탬프를 함께 준다")
    void completedTripDetailReturnsContributionSnapshotAndStamp() throws Exception {
        Long userId = user();
        long itineraryId = create(userId, REGION_A);
        long completedTripId = complete(userId, itineraryId, 50, 2, 320_000);

        mockMvc.perform(get("/api/users/me/completed-trips/{id}", completedTripId).header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTripId").value((int) completedTripId))
                .andExpect(jsonPath("$.itineraryId").value((int) itineraryId))
                .andExpect(jsonPath("$.region.regionId").value((int) REGION_A))
                .andExpect(jsonPath("$.nights").value(1))
                .andExpect(jsonPath("$.contribution.stayHours").value(50))
                .andExpect(jsonPath("$.contribution.partySize").value(2))
                .andExpect(jsonPath("$.contribution.reportedSpending").value(320_000))
                .andExpect(jsonPath("$.contribution.estimatedSpending").value(264_000))
                .andExpect(jsonPath("$.contribution.populationContributionDays").value(2))
                .andExpect(jsonPath("$.contribution.policyVersion").value(POLICY_VERSION))
                .andExpect(jsonPath("$.stamp.collected").value(true))
                .andExpect(jsonPath("$.stamp.visitCount").value(1));
    }

    @Test
    @DisplayName("스탬프 현황은 미수집 지역까지 전체 지역을 내려준다")
    void stampsReturnsAllRegionsIncludingUncollected() throws Exception {
        Long userId = user();
        complete(userId, create(userId, REGION_A), 24, 1, 0);

        int totalRegionCount = (int) regions.count();

        mockMvc.perform(get("/api/users/me/stamps").header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRegionCount").value(totalRegionCount))
                .andExpect(jsonPath("$.stamps.length()").value(totalRegionCount))
                .andExpect(jsonPath("$.collectedCount").value(1))
                .andExpect(jsonPath("$.totalVisitCount").value(1))
                .andExpect(jsonPath("$.stamps[?(@.regionId == " + REGION_A + ")].collected").value(true))
                .andExpect(jsonPath("$.stamps[?(@.regionId == " + REGION_B + ")].collected").value(false))
                .andExpect(jsonPath("$.stamps[?(@.regionId == " + REGION_B + ")].visitCount").value(0));
    }

    @Test
    @DisplayName("같은 지역을 다시 방문하면 스탬프 방문 횟수가 늘어난다")
    void revisitingRegionIncrementsStampVisitCount() throws Exception {
        Long userId = user();
        complete(userId, create(userId, REGION_A), 24, 1, 0);
        complete(userId, create(userId, REGION_A), 24, 1, 0);

        mockMvc.perform(get("/api/users/me/stamps").header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectedCount").value(1))
                .andExpect(jsonPath("$.totalVisitCount").value(2))
                .andExpect(jsonPath("$.stamps[?(@.regionId == " + REGION_A + ")].visitCount").value(2));
    }

    @Test
    @DisplayName("기여도 요약의 합계는 완료 여행 목록의 값을 그대로 더한 값이다")
    void contributionSummaryMatchesSumOfCompletedTrips() throws Exception {
        Long userId = user();
        complete(userId, create(userId, REGION_A), 50, 2, 320_000);
        complete(userId, create(userId, REGION_B), 24, 1, 1_000);

        JsonNode trips = readJson(get("/api/users/me/completed-trips").header("Authorization", token(userId)));
        long expectedSpending = 0;
        int expectedDays = 0;
        int expectedStayHours = 0;
        for (JsonNode trip : trips.get("completedTrips")) {
            expectedSpending += trip.get("estimatedSpending").asLong();
            expectedDays += trip.get("populationContributionDays").asInt();
            expectedStayHours += trip.get("stayHours").asInt();
        }

        JsonNode summary = readJson(get("/api/users/me/contribution-summary").header("Authorization", token(userId)));

        assertThat(summary.get("visitedRegionCount").asInt()).isEqualTo(2);
        assertThat(summary.get("completedTripCount").asInt()).isEqualTo(2);
        assertThat(summary.get("totalStayHours").asInt()).isEqualTo(expectedStayHours).isEqualTo(74);
        assertThat(summary.get("totalReportedSpending").asLong()).isEqualTo(321_000L);
        assertThat(summary.get("totalEstimatedSpending").asLong()).isEqualTo(expectedSpending).isEqualTo(330_000L);
        assertThat(summary.get("totalPopulationContributionDays").asInt()).isEqualTo(expectedDays).isEqualTo(3);
        assertThat(summary.get("policyVersion").asText()).isEqualTo(POLICY_VERSION);

        // 지역별 누계 합도 전체 누계와 일치해야 한다
        long regionSpendingSum = 0;
        for (JsonNode region : summary.get("regions")) {
            regionSpendingSum += region.get("estimatedSpending").asLong();
        }
        assertThat(regionSpendingSum).isEqualTo(expectedSpending);
    }

    @Test
    @DisplayName("같은 지역 여행이 여러 건이면 지역별 누계로 합산된다")
    void contributionSummaryAggregatesTripsOfSameRegion() throws Exception {
        Long userId = user();
        complete(userId, create(userId, REGION_A), 24, 1, 1_000);
        complete(userId, create(userId, REGION_A), 27, 2, 2_000);

        JsonNode summary = readJson(get("/api/users/me/contribution-summary").header("Authorization", token(userId)));

        assertThat(summary.get("visitedRegionCount").asInt()).isEqualTo(1);
        assertThat(summary.get("completedTripCount").asInt()).isEqualTo(2);
        assertThat(summary.get("regions")).hasSize(1);

        JsonNode region = summary.get("regions").get(0);
        assertThat(region.get("tripCount").asInt()).isEqualTo(2);
        assertThat(region.get("stayHours").asInt()).isEqualTo(51);
        // 24시간 1인 → 1일, 27시간 2인 → 2일
        assertThat(region.get("populationContributionDays").asInt()).isEqualTo(3);
        assertThat(region.get("estimatedSpending").asLong()).isEqualTo(DAILY_SPENDING * 1 * 1 + DAILY_SPENDING * 2 * 2);
    }

    @Test
    @DisplayName("기여도 스냅샷이 없는 과거 기록은 조회 시점에 현재 정책으로 계산해 채운다")
    void recalculatesContributionWhenSnapshotIsMissing() throws Exception {
        Long userId = user();
        long completedTripId = complete(userId, create(userId, REGION_A), 50, 2, 320_000);

        jdbcTemplate.update("""
                update completed_trips
                set estimated_spending = 0, population_contribution_days = 0, contribution_policy_version = null
                where completed_trip_id = ?
                """, completedTripId);

        mockMvc.perform(get("/api/users/me/completed-trips/{id}", completedTripId).header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contribution.estimatedSpending").value(264_000))
                .andExpect(jsonPath("$.contribution.populationContributionDays").value(2))
                .andExpect(jsonPath("$.contribution.policyVersion").value(POLICY_VERSION));

        // 저장된 스냅샷은 건드리지 않는다
        Long storedDays = jdbcTemplate.queryForObject(
                "select population_contribution_days from completed_trips where completed_trip_id = ?",
                Long.class, completedTripId);
        assertThat(storedDays).isZero();
    }

    @Test
    @DisplayName("저장 일정 목록은 책갈피한 일정만 내려준다")
    void bookmarkedItinerariesReturnsOnlyBookmarked() throws Exception {
        Long userId = user();
        long bookmarked = create(userId, REGION_A);
        bookmark(userId, bookmarked);
        create(userId, REGION_B);

        mockMvc.perform(get("/api/users/me/bookmarked-itineraries").header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.itineraries[0].itineraryId").value((int) bookmarked))
                .andExpect(jsonPath("$.itineraries[0].region.regionId").value((int) REGION_A))
                .andExpect(jsonPath("$.itineraries[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.itineraries[0].bookmarkedAt").isNotEmpty());
    }

    @Test
    @DisplayName("인증 없이 호출하면 리다이렉트가 아니라 401을 반환한다")
    void requiresAuthentication() throws Exception {
        for (String path : List.of("/completed-trips", "/stamps", "/contribution-summary", "/bookmarked-itineraries")) {
            mockMvc.perform(get("/api/users/me" + path)).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get("/api/users/me/completed-trips/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("남의 여행 기록은 403, 없는 기록은 404를 반환한다")
    void rejectsOtherUsersTripAndUnknownTrip() throws Exception {
        Long ownerId = user();
        long completedTripId = complete(ownerId, create(ownerId, REGION_A), 24, 1, 0);

        mockMvc.perform(get("/api/users/me/completed-trips/{id}", completedTripId).header("Authorization", token(user())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPLETED_TRIP_ACCESS_DENIED"));

        mockMvc.perform(get("/api/users/me/completed-trips/{id}", 999_999L).header("Authorization", token(ownerId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMPLETED_TRIP_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ===== helpers =====

    private void ensureRegion(long regionId, String name) {
        if (!regions.existsById(regionId)) {
            regions.save(new Region(regionId, name, null, null, "identity", null));
        }
    }

    private Long user() {
        return users.save(ItineraryTestFixture.user()).getId();
    }

    private long create(Long userId, long regionId) throws Exception {
        String body = "{\"regionId\":" + regionId + ",\"startDate\":\"" + START_DATE
                + "\",\"nights\":1,\"preferenceTags\":[\"FOOD\"],\"companionType\":\"FRIENDS\"}";
        String response = mockMvc.perform(post("/api/itineraries")
                        .header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("itineraryId").asLong();
    }

    private void bookmark(Long userId, long itineraryId) throws Exception {
        mockMvc.perform(put("/api/itineraries/{id}/bookmark", itineraryId).header("Authorization", token(userId)))
                .andExpect(status().isOk());
    }

    private long complete(Long userId, long itineraryId, int stayHours, int partySize, long totalSpent) throws Exception {
        String body = "{\"stayHours\":" + stayHours + ",\"partySize\":" + partySize + ",\"totalSpent\":" + totalSpent + "}";
        String response = mockMvc.perform(post("/api/itineraries/{id}/completion", itineraryId)
                        .header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("completedTripId").asLong();
    }

    private JsonNode readJson(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        String response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String token(Long userId) {
        return "Bearer test-user-" + userId;
    }

    private void stubAuthenticationFilter() throws Exception {
        doAnswer(invocation -> {
            var request = (jakarta.servlet.http.HttpServletRequest) invocation.getArgument(0);
            SecurityContextHolder.clearContext();
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer test-user-")) {
                Long userId = Long.valueOf(authorization.substring("Bearer test-user-".length()));
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
            }
            ((FilterChain) invocation.getArgument(2)).doFilter(
                    (ServletRequest) invocation.getArgument(0), (ServletResponse) invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }
}
