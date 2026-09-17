package com.travel.meomulkyung.itinerary.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ItineraryRouteResponses {

    private ItineraryRouteResponses() {
    }

    /**
     * @param mode          CAR | TRANSIT
     * @param notice        화면에 반드시 함께 보여줄 한계 문구
     * @param source        데이터 출처
     * @param regionTransit 지역 버스 안내. TRANSIT 조회에서만 값이 있고, 자동차는 null
     * @param fetchedAt     조회 시각. 결과는 저장하지 않고 요청마다 새로 조회한다.
     */
    public record DayRoutes(Long itineraryId, int dayNumber, LocalDate date, String mode,
                            List<Segment> segments, String notice, String source,
                            RegionTransit regionTransit, OffsetDateTime fetchedAt) {
    }

    /**
     * 지역 단위 버스 안내. 외부 호출 없이 설정값으로 내려준다.
     *
     * @param timetable 공식 버스 시간표 안내 페이지. 설정에 없으면 null
     * @param freeBus   관내 버스가 무료인 지역이면 값이 있고, 아니면 null
     * @param checkedOn 링크와 요금 정책을 마지막으로 확인한 날짜
     */
    public record RegionTransit(Timetable timetable, FreeBus freeBus, LocalDate checkedOn) {
    }

    /** @param source 안내 주체 표기 */
    public record Timetable(String url, String source) {
    }

    /**
     * 구간 요금({@link Segment#fare()})과는 별개다. 지역은 무료여도 시·군을 넘는 노선은 유료일 수 있어
     * 정책 안내와 실측 요금을 함께 보여준다.
     *
     * @param label   배지 문구
     * @param caution 함께 보여줄 예외 안내
     * @param basis   무료 판단의 근거 표기. 요금 정책은 조례로 바뀔 수 있다.
     */
    public record FreeBus(String label, String caution, String basis) {
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
