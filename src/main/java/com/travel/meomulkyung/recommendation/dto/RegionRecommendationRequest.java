package com.travel.meomulkyung.recommendation.dto;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegionRecommendationRequest(
        @NotEmpty
        @Size(max = 3)
        List<PreferenceTag> preferenceTags,
        @NotNull
        CompanionType companionType,
        @NotNull
        @Min(1)
        @Max(7)
        Integer nights
) {

    public boolean hasDuplicatePreferenceTags() {
        return preferenceTags != null && preferenceTags.stream().distinct().count() != preferenceTags.size();
    }
}
