package com.travel.meomulkyung.region.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class RegionResponses {

    private RegionResponses() {
    }

    public record ListItem(
            Long regionId,
            String regionName,
            String thumbnailUrl,
            String identityStatement,
            List<Option> representativeTags
    ) {
    }

    public record Detail(
            Long regionId,
            String regionName,
            String heroImageUrl,
            String identityStatement,
            String description,
            List<RepresentativeResource> representativeResources,
            TravelStyle travelStyle,
            List<String> localTips,
            List<SourceAttribution> sourceAttributions,
            LocalDateTime lastUpdatedAt
    ) {
    }

    public record Option(String code, String label) {
    }

    public record RepresentativeResource(
            Long placeId,
            String placeName,
            String category,
            String imageUrl,
            String shortDescription
    ) {
    }

    public record TravelStyle(List<Option> keywords, List<Option> recommendedCompanions) {
    }

    public record SourceAttribution(String sourceName) {
    }
}
