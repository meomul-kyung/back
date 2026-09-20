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

    /**
     * 지역 추천 점수.
     *
     * @param stayFit 숙박일수별 체류 강도를 운영 지역 전체 기준으로 0~1 정규화한 값.
     *                수요 강도 조회에 실패했거나 정규화가 불가능하면 {@code null}이며,
     *                이때는 {@code profile}의 하드코딩 범위로 되돌아간다.
     */
    public double calculate(List<RegionTagScore> regionTagScores, RegionRecommendationProfile profile,
                            List<PreferenceTag> preferenceTags, CompanionType companionType, int nights,
                            Double stayFit) {
        int preferenceScore = preferenceTags.stream()
                .mapToInt(tag -> scoreFor(regionTagScores, tag.getCode()) * RegionRecommendationProfiles.PREFERENCE_TAG_WEIGHT)
                .sum();
        int companionScore = scoreFor(regionTagScores, companionType.getCode());
        return preferenceScore + companionScore + stayDurationScore(profile, nights, stayFit);
    }

    public int scoreFor(List<RegionTagScore> regionTagScores, String tagCode) {
        return regionTagScores.stream()
                .filter(regionTagScore -> regionTagScore.getTag().getCode().equals(tagCode))
                .mapToInt(RegionTagScore::getScore)
                .findFirst()
                .orElse(0);
    }

    /**
     * 체류 기간 적합도.
     *
     * <p>수요 강도를 쓸 수 있으면 실제 관측된 숙박일수별 체류 강도를 쓴다. 범위 안이면 일괄
     * 가점을 주던 기존 방식과 달리, 같은 범위 안에서도 1박이 어울리는 지역과 3박이 어울리는
     * 지역이 갈린다.
     *
     * <p>가중치는 전체 조건 조합을 돌려 정한 값이다. 이보다 낮으면 사용자가 박수를 바꿔도
     * 순위가 거의 그대로고, 크게 높이면 취향 태그보다 세져 "취향 기반 추천"이 흐려진다.
     * 현재 값은 기존 방식과 비슷한 정도로 순위를 흔들면서 근거만 실측으로 바꾼 지점이다.
     */
    private double stayDurationScore(RegionRecommendationProfile profile, int nights, Double stayFit) {
        if (stayFit == null) {
            return nights >= profile.idealMinimumNights() && nights <= profile.idealMaximumNights()
                    ? RegionRecommendationProfiles.STAY_DURATION_MATCH_SCORE
                    : 0;
        }
        return stayFit * RegionRecommendationProfiles.STAY_FIT_WEIGHT;
    }
}
