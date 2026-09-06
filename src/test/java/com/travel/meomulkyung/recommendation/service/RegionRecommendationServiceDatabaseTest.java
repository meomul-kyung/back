package com.travel.meomulkyung.recommendation.service;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.recommendation.dto.RegionRecommendationRequest;
import com.travel.meomulkyung.region.service.RegionSeedDataInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({RegionSeedDataInitializer.class, RegionRecommendationScoreCalculator.class, RegionRecommendationService.class})
class RegionRecommendationServiceDatabaseTest {

    @Autowired
    private RegionSeedDataInitializer initializer;

    @Autowired
    private RegionRecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        initializer.initialize();
    }

    @Test
    void returnsThreeRecommendationsFromRepositoryDataInStableOrder() {
        RegionRecommendationRequest request = new RegionRecommendationRequest(
                List.of(PreferenceTag.NATURE, PreferenceTag.FOOD, PreferenceTag.WALKING), CompanionType.FRIENDS, 2);

        var first = recommendationService.recommend(request);
        var second = recommendationService.recommend(request);

        assertThat(first.recommendations()).hasSize(3);
        assertThat(first.recommendations())
                .extracting(recommendation -> recommendation.rank())
                .containsExactly(1, 2, 3);
        assertThat(second.recommendations()).isEqualTo(first.recommendations());
    }
}
