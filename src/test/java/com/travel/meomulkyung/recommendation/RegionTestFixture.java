package com.travel.meomulkyung.recommendation;

import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.domain.RegionTagScore;
import com.travel.meomulkyung.region.domain.Tag;
import com.travel.meomulkyung.region.domain.TagType;

import java.util.List;

public final class RegionTestFixture {

    private RegionTestFixture() {
    }

    public static List<Region> regionsFromSeedProfiles() {
        return RegionRecommendationProfiles.ALL.stream()
                .map(profile -> {
                    Region region = new Region(profile.regionId(), profile.regionName(), null, null,
                            profile.identityStatement(), null);
                    profile.tagScores().forEach((tag, score) -> region.addRegionTagScore(
                            new RegionTagScore(region, new Tag(tag.getCode(), tag.getLabel(), TagType.TASTE), score)));
                    profile.companionScores().forEach((tag, score) -> region.addRegionTagScore(
                            new RegionTagScore(region, new Tag(tag.getCode(), tag.getLabel(), TagType.COMPANION), score)));
                    return region;
                })
                .toList();
    }
}
