package com.travel.meomulkyung.recommendation.service;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.domain.RegionTagScore;
import com.travel.meomulkyung.region.domain.Tag;
import com.travel.meomulkyung.region.domain.TagType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegionRecommendationScoreCalculatorTest {

    private final RegionRecommendationScoreCalculator calculator = new RegionRecommendationScoreCalculator();

    @Test
    void calculatesPreferenceCompanionAndStayScores() {
        var andong = RegionRecommendationProfiles.ALL.getFirst();
        Region region = new Region(andong.regionId(), andong.regionName(), null, null, andong.identityStatement(), null);
        List<RegionTagScore> scores = List.of(
                new RegionTagScore(region, new Tag("FOOD", "food", TagType.TASTE), 9),
                new RegionTagScore(region, new Tag("WALKING", "walking", TagType.TASTE), 7),
                new RegionTagScore(region, new Tag("FRIENDS", "friends", TagType.COMPANION), 5)
        );

        int score = calculator.calculate(scores, andong, List.of(PreferenceTag.FOOD, PreferenceTag.WALKING),
                CompanionType.FRIENDS, 2);

        assertThat(score).isEqualTo((9 * 10) + (7 * 10) + 5 + 4);
    }

    @Test
    void doesNotAddStayScoreOutsideTheIdealRange() {
        var ulleung = RegionRecommendationProfiles.ALL.getLast();
        Region region = new Region(ulleung.regionId(), ulleung.regionName(), null, null, ulleung.identityStatement(), null);
        List<RegionTagScore> scores = List.of(
                new RegionTagScore(region, new Tag("SEA", "sea", TagType.TASTE), 10),
                new RegionTagScore(region, new Tag("FRIENDS", "friends", TagType.COMPANION), 5)
        );

        int score = calculator.calculate(scores, ulleung, List.of(PreferenceTag.SEA), CompanionType.FRIENDS, 2);

        assertThat(score).isEqualTo((10 * 10) + 5);
    }
}
