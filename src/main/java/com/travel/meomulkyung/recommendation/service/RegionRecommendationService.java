package com.travel.meomulkyung.recommendation.service;

import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles.RegionRecommendationProfile;
import com.travel.meomulkyung.recommendation.dto.RegionRecommendationRequest;
import com.travel.meomulkyung.recommendation.dto.RegionRecommendationResponse;
import com.travel.meomulkyung.recommendation.external.TourDemandProvider;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegionRecommendationService {

    private final RegionRecommendationScoreCalculator scoreCalculator;
    private final RegionRepository regionRepository;
    private final TourDemandProvider demandProvider;

    public RegionRecommendationResponse recommend(RegionRecommendationRequest request) {
        List<Region> regions = regionRepository.findAllWithTagScores();
        // 정렬 비교자 안에서 점수를 계산하므로, 비교마다 외부 API를 부르지 않도록 미리 받아 둔다.
        Map<Long, Double> stayFits = stayFits(regions, request.nights());

        List<RegionRecommendationResponse.Recommendation> recommendations = regions.stream()
                .sorted(Comparator
                        .comparingDouble((Region region) -> scoreCalculator.calculate(region.getRegionTagScores(),
                                displayProfile(region), request.preferenceTags(), request.companionType(),
                                request.nights(), stayFits.get(region.getId())))
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

    /**
     * 숙박일수별 체류 강도를 후보 지역 전체 기준으로 0~1 정규화한다.
     *
     * <p>정규화는 비교 대상이 모두 갖춰져야 의미가 있다. 일부 지역만 값이 있으면 없는 지역이
     * 이유 없이 0점을 받게 되므로, 하나라도 빠지면 빈 Map을 돌려 전체를 기존 하드코딩 범위
     * 방식으로 되돌린다. 외부 API 장애가 추천 순위를 왜곡하는 것보다 낫다.
     */
    private Map<Long, Double> stayFits(List<Region> regions, int nights) {
        Map<Long, TourDemandProvider.StayProfile> profiles = demandProvider.findStayProfiles();
        if (regions.size() < 2 || !profiles.keySet().containsAll(regions.stream().map(Region::getId).toList())) {
            return Map.of();
        }

        List<Double> values = regions.stream().map(region -> profiles.get(region.getId()).valueFor(nights)).toList();
        double lowest = values.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        double highest = values.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        if (highest == lowest) {
            return Map.of();
        }

        Map<Long, Double> fits = new HashMap<>();
        regions.forEach(region -> fits.put(region.getId(),
                (profiles.get(region.getId()).valueFor(nights) - lowest) / (highest - lowest)));
        return Map.copyOf(fits);
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
