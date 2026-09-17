package com.travel.meomulkyung.itinerary.service;

import com.travel.meomulkyung.itinerary.ItineraryTestConfig;
import com.travel.meomulkyung.itinerary.ItineraryTestFixture;
import com.travel.meomulkyung.itinerary.domain.ItineraryItemType;
import com.travel.meomulkyung.itinerary.dto.ItineraryRequests;
import com.travel.meomulkyung.itinerary.dto.ItineraryResponses;
import com.travel.meomulkyung.itinerary.external.TourPlaceProvider;
import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.region.repository.RegionRepository;
import com.travel.meomulkyung.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 좌표 기반 배치 검증.
 *
 * <p>기존 생성 테스트({@link DailyPlanGenerationTest})의 가짜 장소는 좌표가 모두 없어
 * "좌표를 모를 때는 기존 순서" 경로만 탄다. 그래서 좌표가 있는 경우는 여기서 따로 검증한다.
 *
 * <p>후보는 일부러 서쪽·동쪽을 <b>번갈아</b> 넣는다. 그래야 배치가 목록 순서가 아니라
 * 좌표를 보고 결정됐는지 구분할 수 있다.
 */
@DataJpaTest
@Import({ItineraryService.class, ItineraryTestConfig.class, com.travel.meomulkyung.contribution.ContributionPolicy.class})
class RouteAwareGenerationTest {

    /** 서쪽 군집(안동 부근)과 동쪽 군집(울진 부근)의 경계. */
    private static final double BORDER_LONGITUDE = 129.0;

    @Autowired ItineraryService service;
    @Autowired UserRepository users;
    @Autowired RegionRepository regions;
    @Autowired ItineraryTestConfig.FakeTourPlaceProvider places;
    @Autowired ItineraryTestConfig.FakeFestivalProvider festivals;
    @Autowired Clock clock;

    @BeforeEach
    void setUp() {
        regions.save(ItineraryTestFixture.region());
        places.reset();
        festivals.setFestivals(List.of());
    }

    @Test
    void groupsNearbyPlacesIntoTheSameDay() {
        places.setCandidates(interleavedTwoClusters());

        ItineraryResponses.Itinerary response = service.create(user(), request(1));

        for (ItineraryResponses.Day day : response.days()) {
            List<Double> longitudes = visits(day).stream()
                    .map(ItineraryResponses.Item::longitude).filter(Objects::nonNull).toList();
            assertThat(longitudes).hasSize(5);
            boolean west = longitudes.getFirst() < BORDER_LONGITUDE;
            assertThat(longitudes)
                    .as("하루 안의 장소가 한 군집에 모여야 한다 (번갈아 넣은 목록 순서라면 섞인다)")
                    .allMatch(longitude -> (longitude < BORDER_LONGITUDE) == west);
        }
    }

    @Test
    void keepsDailyShapeAndPlaceCountWhenCoordinatesArePresent() {
        places.setCandidates(interleavedTwoClusters());

        ItineraryResponses.Itinerary response = service.create(user(), request(1));

        assertThat(response.days()).hasSize(2);
        for (ItineraryResponses.Day day : response.days()) {
            assertThat(visits(day)).extracting(ItineraryResponses.Item::type)
                    .containsExactly("TOURIST_SPOT", "RESTAURANT", "TOURIST_SPOT", "RESTAURANT", "TOURIST_SPOT");
        }
        assertThat(placeIds(response)).doesNotHaveDuplicates().hasSize(10);
    }

    @Test
    void placesWithoutCoordinatesStillGenerateNormally() {
        List<TourPlaceProvider.Place> pool = new ArrayList<>(interleavedTwoClusters());
        pool.set(0, new TourPlaceProvider.Place(1L, "좌표없는 관광지", ItineraryItemType.TOURIST_SPOT, "i", "a", null, null));
        pool.set(7, new TourPlaceProvider.Place(108L, "좌표없는 식당", ItineraryItemType.RESTAURANT, "i", "a", null, null));
        places.setCandidates(pool);

        ItineraryResponses.Itinerary response = service.create(user(), request(1));

        assertThat(response.days()).hasSize(2);
        assertThat(placeIds(response)).doesNotHaveDuplicates().hasSize(10);
    }

    @Test
    void regenerateStillSwapsEveryPlace() {
        places.setCandidates(roomyTwoClusters());
        Long userId = user();
        ItineraryResponses.Itinerary first = service.create(userId, request(1));

        ItineraryResponses.Itinerary second =
                service.regenerate(userId, first.itineraryId(), new ItineraryRequests.Replace(false));

        assertThat(second.generationVersion()).isEqualTo(2);
        assertThat(placeIds(second)).doesNotContainAnyElementsOf(placeIds(first));
    }

    @Test
    void replaceChoosesTheNearestCandidateAndKeepsTheSlot() {
        places.setCandidates(singleLineWithSpareCandidates());
        Long userId = user();
        ItineraryResponses.Itinerary created = service.create(userId, request(1));
        ItineraryResponses.Item target = visits(created.days().getFirst()).getFirst();

        ItineraryResponses.Replacement replacement =
                service.replace(userId, created.itineraryId(), target.itemId(), new ItineraryRequests.Replace(null));

        assertThat(replacement.newItem().placeId()).isEqualTo(900L);
        assertThat(replacement.newItem().sequence()).isEqualTo(target.sequence());
    }

    /**
     * 서쪽 3곳 · 동쪽 3곳, 각 군집 근처 식당 2곳씩. 목록에는 서쪽·동쪽을 번갈아 넣는다.
     *
     * <p>관광 6 · 식당 4로 1박 2일 필요 수량과 정확히 같아 재사용 경고가 뜨지 않고, 좌표 배치 경로를 탄다.
     */
    private static List<TourPlaceProvider.Place> interleavedTwoClusters() {
        return List.of(
                attraction(1L, 36.50, 128.50), attraction(2L, 36.95, 129.40),
                attraction(3L, 36.52, 128.52), attraction(4L, 36.97, 129.42),
                attraction(5L, 36.54, 128.54), attraction(6L, 36.99, 129.44),
                restaurant(101L, 36.51, 128.51), restaurant(102L, 36.96, 129.41),
                restaurant(103L, 36.53, 128.53), restaurant(104L, 36.98, 129.43));
    }

    /**
     * 재생성 검증용. 필요 수량의 두 배를 준비해 회전(generationVersion) 후 겹치지 않게 한다.
     *
     * <p>후보가 필요 수량과 같으면 재생성해도 뽑을 것이 그것뿐이라 같은 장소가 나온다.
     * 그건 좌표 배치와 무관한 {@code fill}의 성질이므로, 여기서는 여유 있는 풀로 검증한다.
     */
    private static List<TourPlaceProvider.Place> roomyTwoClusters() {
        List<TourPlaceProvider.Place> pool = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            pool.add(attraction(1L + index * 2, 36.50 + index * 0.02, 128.50 + index * 0.02));
            pool.add(attraction(2L + index * 2, 36.95 + index * 0.02, 129.40 + index * 0.02));
        }
        for (int index = 0; index < 4; index++) {
            pool.add(restaurant(101L + index * 2, 36.51 + index * 0.02, 128.51 + index * 0.02));
            pool.add(restaurant(102L + index * 2, 36.96 + index * 0.02, 129.41 + index * 0.02));
        }
        return List.copyOf(pool);
    }

    /**
     * 교체 검증용. 관광지를 한 줄로 두고 여분 후보 2곳(가까운 900, 먼 901)을 남긴다.
     *
     * <p>{@code fill}이 generationVersion(=1) × 필요수량(6)부터 순환하며 뽑으므로
     * 목록 4·5번 자리(900·901)만 일정에 쓰이지 않고 교체 후보로 남는다.
     */
    private static List<TourPlaceProvider.Place> singleLineWithSpareCandidates() {
        return List.of(
                attraction(1L, 36.50, 128.50), attraction(2L, 36.52, 128.52),
                attraction(3L, 36.54, 128.54), attraction(4L, 36.56, 128.56),
                attraction(900L, 36.53, 128.53), attraction(901L, 36.50, 129.90),
                attraction(5L, 36.58, 128.58), attraction(6L, 36.60, 128.60),
                restaurant(101L, 36.51, 128.51), restaurant(102L, 36.55, 128.55),
                restaurant(103L, 36.53, 128.53), restaurant(104L, 36.57, 128.57));
    }

    private static TourPlaceProvider.Place attraction(Long id, double latitude, double longitude) {
        return new TourPlaceProvider.Place(id, "관광지 " + id, ItineraryItemType.TOURIST_SPOT,
                "https://image.example/" + id, "주소 " + id, latitude, longitude);
    }

    private static TourPlaceProvider.Place restaurant(Long id, double latitude, double longitude) {
        return new TourPlaceProvider.Place(id, "식당 " + id, ItineraryItemType.RESTAURANT,
                "https://image.example/" + id, "주소 " + id, latitude, longitude);
    }

    private static List<ItineraryResponses.Item> visits(ItineraryResponses.Day day) {
        return day.items().stream().filter(item -> item.placeId() != null || item.festivalId() != null).toList();
    }

    private static List<Long> placeIds(ItineraryResponses.Itinerary response) {
        return response.days().stream().flatMap(day -> day.items().stream())
                .map(ItineraryResponses.Item::placeId).filter(Objects::nonNull).toList();
    }

    private ItineraryRequests.Create request(int nights) {
        return new ItineraryRequests.Create(1L, LocalDate.now(clock), nights,
                List.of(PreferenceTag.FOOD), CompanionType.FRIENDS);
    }

    private Long user() {
        return users.save(ItineraryTestFixture.user()).getId();
    }
}
