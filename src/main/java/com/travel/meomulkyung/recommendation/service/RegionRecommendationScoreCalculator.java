package com.travel.meomulkyung.recommendation.service;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles.RegionRecommendationProfile;
import com.travel.meomulkyung.region.domain.RegionTagScore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RegionRecommendationScoreCalculator {

    public int calculate(List<RegionTagScore> regionTagScores, RegionRecommendationProfile profile, List<PreferenceTag> preferenceTags,
                         CompanionType companionType, int nights) {
        int preferenceScore = preferenceTags.stream()
                .mapToInt(tag -> scoreFor(regionTagScores, tag.getCode()) * RegionRecommendationProfiles.PREFERENCE_TAG_WEIGHT)
                .sum();
        int companionScore = scoreFor(regionTagScores, companionType.getCode());
        return preferenceScore + companionScore + stayDurationScore(profile, nights);
    }

    public int scoreFor(List<RegionTagScore> regionTagScores, String tagCode) {
        return regionTagScores.stream()
                .filter(regionTagScore -> regionTagScore.getTag().getCode().equals(tagCode))
                .mapToInt(RegionTagScore::getScore)
                .findFirst()
                .orElse(0);
    }

    private int stayDurationScore(RegionRecommendationProfile profile, int nights) {
        return nights >= profile.idealMinimumNights() && nights <= profile.idealMaximumNights()
                ? RegionRecommendationProfiles.STAY_DURATION_MATCH_SCORE
                : 0;
    }
}
