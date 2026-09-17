package com.travel.meomulkyung.itinerary.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ItineraryRouteResponses {

    private ItineraryRouteResponses() {
    }

    /**
     * @param mode      CAR | TRANSIT
     * @param notice    화면에 반드시 함께 보여줄 한계 문구
     * @param source    데이터 출처
     * @param fetchedAt 조회 시각. 결과는 저장하지 않고 요청마다 새로 조회한다.
     */
    public record DayRoutes(Long itineraryId, int dayNumber, LocalDate date, String mode,
                            List<Segment> segments, String notice, String source, OffsetDateTime fetchedAt) {
    }

    /**
     * @param status          OK | NEARBY | NO_STOP | NO_ROUTE | UNAVAILABLE
     * @param durationMinutes 이동 소요시간(분). 대중교통은 배차 대기시간 제외
     * @param fare            대중교통 요금(원). 0이면 무료
     * @param free            대중교통 요금이 0원인지 여부. 자동차는 null
     * @param taxiFare        예상 택시 요금(원). 자동차 조회에서만 제공
     * @param summary         대중교통 노선 요약
     * @param path            경로선 [위도, 경도] 목록
     * @param landingUrl      카카오맵 길찾기 링크
     */
    public record Segment(Long fromItemId, String fromTitle, Long toItemId, String toTitle,
                          String status, Integer durationMinutes, Integer distanceMeters, Integer transfers,
                          Integer fare, Boolean free, Integer taxiFare, String summary,
                          List<double[]> path, String landingUrl) {
    }
}
