package com.travel.meomulkyung.recommendation.external;

import java.util.Map;

/**
 * 한국관광공사 「지역별 관광 수요 강도」의 체류 강도 지표를 지역별로 제공한다.
 *
 * <p>지역 추천의 체류 기간 점수는 원래 {@code RegionRecommendationProfiles}의
 * {@code idealMinimumNights ~ idealMaximumNights} 범위에 들면 고정 점수를 주는 방식이었다.
 * 이 범위는 도메인 지식으로 손으로 적은 값이라 근거가 없고, 범위 안이면 1박이든 4박이든
 * 같은 점수라 사용자가 고른 박수가 순위에 거의 영향을 주지 못했다.
 *
 * <p>여기서는 실제 관측값인 숙박일수별 체류 강도(2103 1박 / 2104 2박 / 2105 3박 이상)를
 * 대신 쓴다. 지역마다 "며칠 묵는 곳인가"의 결이 다르다. 예를 들어 울진은 2박 지표가
 * 15개 운영 지역 중 가장 높고, 안동은 3박 이상 지표가 가장 높다.
 *
 * <p>날씨와 마찬가지로 추천의 부가 근거이므로 조회에 실패하면 예외를 던지지 않고 빈 값을
 * 돌려준다. 호출 측은 빈 값을 받으면 기존 하드코딩 범위 방식으로 되돌아간다.
 */
public interface TourDemandProvider {

    /**
     * {@code region_id} 기준 숙박일수별 체류 강도. 조회에 실패하면 빈 Map을 돌려준다.
     * 일부 지역만 조회된 경우에도 그대로 담아 보내고, 정규화 가능 여부는 호출 측이 판단한다.
     */
    Map<Long, StayProfile> findStayProfiles();

    /**
     * 한 지역의 숙박일수별 체류 강도 원값.
     *
     * <p>API가 1박 / 2박 / 3박 이상 세 구간만 제공하므로 4박 이상은 모두
     * {@code threeOrMoreNights} 하나로 묶인다.
     */
    record StayProfile(double oneNight, double twoNights, double threeOrMoreNights) {

        public double valueFor(int nights) {
            if (nights <= 1) {
                return oneNight;
            }
            return nights == 2 ? twoNights : threeOrMoreNights;
        }
    }
}
