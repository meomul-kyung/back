package com.travel.meomulkyung.recommendation.dto;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;

import java.util.List;

public record RegionRecommendationResponse(
        Criteria criteria,
        List<Recommendation> recommendations
) {

    public record Criteria(List<PreferenceTag> preferenceTags, CompanionType companionType, int nights) {
    }

    public record Recommendation(
            int rank,
            long regionId,
            String regionName,
            String thumbnailUrl,
            String identityStatement,
            List<PreferenceTag> matchedTags,
            String recommendationReason,
            List<String> representativePlaces
    ) {
    }
}
