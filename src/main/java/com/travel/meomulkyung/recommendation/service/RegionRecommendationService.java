package com.travel.meomulkyung.recommendation.service;

import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles.RegionRecommendationProfile;
import com.travel.meomulkyung.recommendation.dto.RegionRecommendationRequest;
import com.travel.meomulkyung.recommendation.dto.RegionRecommendationResponse;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionRecommendationService {

    private final RegionRecommendationScoreCalculator scoreCalculator;
    private final RegionRepository regionRepository;

    public RegionRecommendationResponse recommend(RegionRecommendationRequest request) {
        List<RegionRecommendationResponse.Recommendation> recommendations = regionRepository.findAllWithTagScores().stream()
                .sorted(Comparator
                        .comparingInt((Region region) -> scoreCalculator.calculate(region.getRegionTagScores(), displayProfile(region),
                                request.preferenceTags(), request.companionType(), request.nights()))
                        .reversed()
                        .thenComparingLong(Region::getId))
                .limit(3)
                .map(region -> toRecommendation(region, request.preferenceTags()))
                .toList();

        List<RegionRecommendationResponse.Recommendation> rankedRecommendations = java.util.stream.IntStream.range(0, recommendations.size())
                .mapToObj(index -> withRank(index + 1, recommendations.get(index)))
                .toList();

        return new RegionRecommendationResponse(
                new RegionRecommendationResponse.Criteria(request.preferenceTags(), request.companionType(), request.nights()),
                rankedRecommendations
        );
    }

    private RegionRecommendationProfile displayProfile(Region region) {
        return RegionRecommendationProfiles.findByRegionId(region.getId());
    }

    private RegionRecommendationResponse.Recommendation toRecommendation(Region region,
                                                                           List<PreferenceTag> preferenceTags) {
        RegionRecommendationProfile profile = displayProfile(region);
        List<PreferenceTag> matchedTags = preferenceTags.stream()
                .filter(tag -> scoreCalculator.scoreFor(region.getRegionTagScores(), tag.getCode()) > 0)
                .toList();
        return new RegionRecommendationResponse.Recommendation(0, region.getId(), region.getName(), region.getThumbnailUrl(),
                region.getIdentityStatement(), matchedTags, profile.recommendationReason(), profile.representativePlaces());
    }

    private RegionRecommendationResponse.Recommendation withRank(int rank, RegionRecommendationResponse.Recommendation recommendation) {
        return new RegionRecommendationResponse.Recommendation(rank, recommendation.regionId(), recommendation.regionName(),
                recommendation.thumbnailUrl(), recommendation.identityStatement(), recommendation.matchedTags(),
                recommendation.recommendationReason(), recommendation.representativePlaces());
    }
}
