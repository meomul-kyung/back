package com.travel.meomulkyung.recommendation.service;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.domain.RegionTagScore;
import com.travel.meomulkyung.region.domain.Tag;
import com.travel.meomulkyung.region.domain.TagType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegionRecommendationScoreCalculatorTest {

    private final RegionRecommendationScoreCalculator calculator = new RegionRecommendationScoreCalculator();

    @Test
    @DisplayName("수요 강도를 쓸 수 없으면 이상 박수 범위에 드는지로 가점한다")
    void calculatesPreferenceCompanionAndStayScores() {
        var andong = RegionRecommendationProfiles.ALL.getFirst();
        Region region = new Region(andong.regionId(), andong.regionName(), null, null, andong.identityStatement(), null);
        List<RegionTagScore> scores = List.of(
                new RegionTagScore(region, new Tag("FOOD", "food", TagType.TASTE), 9),
                new RegionTagScore(region, new Tag("WALKING", "walking", TagType.TASTE), 7),
                new RegionTagScore(region, new Tag("FRIENDS", "friends", TagType.COMPANION), 5)
        );

        double score = calculator.calculate(scores, andong, List.of(PreferenceTag.FOOD, PreferenceTag.WALKING),
                CompanionType.FRIENDS, 2, null);

        assertThat(score).isEqualTo((9 * 10) + (7 * 10) + (5 * RegionRecommendationProfiles.COMPANION_WEIGHT) + 4);
    }

    @Test
    @DisplayName("수요 강도를 쓸 수 없고 이상 박수 범위를 벗어나면 체류 점수가 붙지 않는다")
    void doesNotAddStayScoreOutsideTheIdealRange() {
        var ulleung = RegionRecommendationProfiles.ALL.getLast();
        Region region = new Region(ulleung.regionId(), ulleung.regionName(), null, null, ulleung.identityStatement(), null);
        List<RegionTagScore> scores = List.of(
                new RegionTagScore(region, new Tag("SEA", "sea", TagType.TASTE), 10),
                new RegionTagScore(region, new Tag("FRIENDS", "friends", TagType.COMPANION), 5)
        );

        double score = calculator.calculate(scores, ulleung, List.of(PreferenceTag.SEA), CompanionType.FRIENDS, 2, null);

        assertThat(score).isEqualTo((10 * 10) + (5 * RegionRecommendationProfiles.COMPANION_WEIGHT));
    }

    @Test
    @DisplayName("수요 강도가 있으면 이상 박수 범위 대신 정규화된 체류 강도로 가점한다")
    void usesStayFitInsteadOfHardcodedRangeWhenDemandIsAvailable() {
        var ulleung = RegionRecommendationProfiles.ALL.getLast();
        Region region = new Region(ulleung.regionId(), ulleung.regionName(), null, null, ulleung.identityStatement(), null);
        List<RegionTagScore> scores = List.of(
                new RegionTagScore(region, new Tag("SEA", "sea", TagType.TASTE), 10),
                new RegionTagScore(region, new Tag("FRIENDS", "friends", TagType.COMPANION), 5)
        );

        // 2박은 울릉의 이상 박수 범위(3~5박) 밖이라 폴백이었다면 0점이지만, 실측 체류 강도는 별개다.
        double score = calculator.calculate(scores, ulleung, List.of(PreferenceTag.SEA), CompanionType.FRIENDS, 2, 1.0);

        assertThat(score).isEqualTo((10 * 10) + (5 * RegionRecommendationProfiles.COMPANION_WEIGHT) + RegionRecommendationProfiles.STAY_FIT_WEIGHT);
    }

    @Test
    @DisplayName("체류 강도가 최하위인 지역은 체류 점수가 0이 된다")
    void lowestStayFitAddsNothing() {
        var andong = RegionRecommendationProfiles.ALL.getFirst();
        Region region = new Region(andong.regionId(), andong.regionName(), null, null, andong.identityStatement(), null);
        List<RegionTagScore> scores = List.of(
                new RegionTagScore(region, new Tag("FOOD", "food", TagType.TASTE), 9),
                new RegionTagScore(region, new Tag("FRIENDS", "friends", TagType.COMPANION), 5)
        );

        double score = calculator.calculate(scores, andong, List.of(PreferenceTag.FOOD), CompanionType.FRIENDS, 2, 0.0);

        assertThat(score).isEqualTo((9 * 10) + (5 * RegionRecommendationProfiles.COMPANION_WEIGHT));
    }
}
