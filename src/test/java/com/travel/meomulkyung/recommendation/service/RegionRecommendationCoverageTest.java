package com.travel.meomulkyung.recommendation.service;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles.RegionRecommendationProfile;
import com.travel.meomulkyung.recommendation.external.TourDemandProvider.StayProfile;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.domain.RegionTagScore;
import com.travel.meomulkyung.region.domain.Tag;
import com.travel.meomulkyung.region.domain.TagType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 조건 조합 전체를 돌려 어떤 운영 지역도 추천에서 구조적으로 배제되지 않는지 확인한다.
 *
 * <p>15개 인구감소지역 전체가 서비스 대상이므로, 특정 지역이 어떤 조건으로도 상위 3위 안에
 * 들지 못하면 그 지자체는 서비스에 존재하지 않는 것과 같다. 체류 강도 가중치를 올리면 실제로
 * 이런 사각지대가 생기므로, 가중치를 조정할 때 이 테스트가 먼저 깨지도록 둔다.
 */
class RegionRecommendationCoverageTest {

    private final RegionRecommendationScoreCalculator calculator = new RegionRecommendationScoreCalculator();

    /** baseYm=202608 실측값. region_id → (1박, 2박, 3박 이상) 체류 강도. */
    private static final Map<Long, StayProfile> STAY_PROFILES = Map.ofEntries(
            Map.entry(1L, new StayProfile(71.35, 74.35, 77.41)),
            Map.entry(2L, new StayProfile(62.68, 67.73, 68.30)),
            Map.entry(3L, new StayProfile(66.88, 69.53, 64.11)),
            Map.entry(4L, new StayProfile(63.16, 66.93, 66.82)),
            Map.entry(5L, new StayProfile(56.79, 62.87, 61.40)),
            Map.entry(6L, new StayProfile(53.64, 59.47, 59.55)),
            Map.entry(7L, new StayProfile(57.21, 62.54, 60.25)),
            Map.entry(8L, new StayProfile(56.66, 60.93, 60.98)),
            Map.entry(9L, new StayProfile(64.18, 63.72, 60.37)),
            Map.entry(10L, new StayProfile(60.93, 62.88, 65.73)),
            Map.entry(11L, new StayProfile(69.52, 80.90, 70.43)),
            Map.entry(12L, new StayProfile(75.34, 79.28, 65.21)),
            Map.entry(13L, new StayProfile(53.80, 58.26, 58.80)),
            Map.entry(14L, new StayProfile(58.94, 60.79, 60.05)),
            Map.entry(15L, new StayProfile(51.69, 64.66, 64.54))
    );

    @Test
    @DisplayName("모든 조건 조합에서 15개 지역이 각각 최소 한 번은 상위 3위에 든다")
    void everyRegionIsRecommendableUnderSomeCondition() {
        Map<Long, List<RegionTagScore>> scoresByRegion = tagScores();
        Map<String, Integer> appearances = new LinkedHashMap<>();
        RegionRecommendationProfiles.ALL.forEach(profile -> appearances.put(profile.regionName(), 0));

        for (List<PreferenceTag> preferenceTags : preferenceCombinations()) {
            for (CompanionType companionType : CompanionType.values()) {
                for (int nights = 1; nights <= 7; nights++) {
                    for (RegionRecommendationProfile profile : topThree(scoresByRegion, preferenceTags, companionType, nights)) {
                        appearances.merge(profile.regionName(), 1, Integer::sum);
                    }
                }
            }
        }

        assertThat(appearances).allSatisfy((regionName, count) ->
                assertThat(count).describedAs("%s 가 한 번도 추천되지 않았다", regionName).isPositive());
    }

    private List<RegionRecommendationProfile> topThree(Map<Long, List<RegionTagScore>> scoresByRegion,
                                                       List<PreferenceTag> preferenceTags,
                                                       CompanionType companionType, int nights) {
        Map<Long, Double> stayFits = stayFits(nights);
        return RegionRecommendationProfiles.ALL.stream()
                .sorted(Comparator
                        .comparingDouble((RegionRecommendationProfile profile) -> calculator.calculate(
                                scoresByRegion.get(profile.regionId()), profile, preferenceTags, companionType, nights,
                                stayFits.get(profile.regionId())))
                        .reversed()
                        .thenComparingLong(RegionRecommendationProfile::regionId))
                .limit(3)
                .toList();
    }

    /** RegionRecommendationService 의 정규화와 같은 방식이다. */
    private Map<Long, Double> stayFits(int nights) {
        List<Double> values = STAY_PROFILES.values().stream().map(profile -> profile.valueFor(nights)).toList();
        double lowest = values.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        double highest = values.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        Map<Long, Double> fits = new HashMap<>();
        STAY_PROFILES.forEach((regionId, profile) ->
                fits.put(regionId, (profile.valueFor(nights) - lowest) / (highest - lowest)));
        return fits;
    }

    private Map<Long, List<RegionTagScore>> tagScores() {
        Map<Long, List<RegionTagScore>> scores = new HashMap<>();
        for (RegionRecommendationProfile profile : RegionRecommendationProfiles.ALL) {
            Region region = new Region(profile.regionId(), profile.regionName(), null, null, profile.identityStatement(), null);
            List<RegionTagScore> regionScores = new ArrayList<>();
            profile.tagScores().forEach((tag, score) ->
                    regionScores.add(new RegionTagScore(region, new Tag(tag.getCode(), tag.getLabel(), TagType.TASTE), score)));
            profile.companionScores().forEach((companion, score) ->
                    regionScores.add(new RegionTagScore(region, new Tag(companion.getCode(), companion.getLabel(), TagType.COMPANION), score)));
            scores.put(profile.regionId(), List.copyOf(regionScores));
        }
        return scores;
    }

    /** 취향 태그는 1~3개를 고를 수 있다. 중복 없는 모든 조합을 만든다. */
    private List<List<PreferenceTag>> preferenceCombinations() {
        PreferenceTag[] tags = PreferenceTag.values();
        List<List<PreferenceTag>> combinations = new ArrayList<>();
        for (int first = 0; first < tags.length; first++) {
            combinations.add(List.of(tags[first]));
            for (int second = first + 1; second < tags.length; second++) {
                combinations.add(List.of(tags[first], tags[second]));
                for (int third = second + 1; third < tags.length; third++) {
                    combinations.add(List.of(tags[first], tags[second], tags[third]));
                }
            }
        }
        return combinations;
    }
}
