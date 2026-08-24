package com.travel.meomulkyung.recommendation.dto;

import java.util.List;

public record TravelOptionsResponse(
        List<Option> preferenceTags,
        PreferenceSelection preferenceSelection,
        List<Option> companionTypes,
        StayDuration stayDuration
) {

    public record Option(String code, String label) {
    }

    public record PreferenceSelection(int minimum, int maximum) {
    }

    public record StayDuration(int minimumNights, int maximumNights) {
    }
}
