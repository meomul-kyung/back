package com.travel.meomulkyung.itinerary.external;

import java.util.List;

/**
 * 두 지점 사이의 이동 경로를 조회한다.
 *
 * <p>구현체는 외부 API 실패를 예외로 던지지 않고 {@link RouteStatus#UNAVAILABLE}로 돌려준다.
 * 구간 하나의 실패가 하루 전체 응답을 막지 않게 하기 위함이다.
 */
public interface RouteProvider {

    Route car(Point from, Point to);

    Route transit(Point from, Point to);

    record Point(double latitude, double longitude) { }

    enum RouteStatus {
        /** 경로 조회 성공 */
        OK,
        /** 두 지점이 매우 가까워 API를 호출하지 않음 (도보 이동 권장) */
        NEARBY,
        /** 출발지 또는 도착지 주변에 정류장이 없음 (대중교통) */
        NO_STOP,
        /** 경로를 찾지 못함 */
        NO_ROUTE,
        /** 키 미설정, 타임아웃, 외부 API 오류 등으로 조회 실패 */
        UNAVAILABLE
    }

    /**
     * @param durationSeconds 이동 소요시간(초). 대중교통은 배차 대기시간이 포함되지 않는다.
     * @param fare            대중교통 요금(원). 자동차는 null
     * @param taxiFare        예상 택시 요금(원). 대중교통은 null
     * @param summary         대중교통 노선 요약. 자동차는 null
     * @param path            경로선 좌표 [위도, 경도] 목록
     * @param landingUrl      카카오맵 길찾기 링크
     */
    record Route(RouteStatus status, Integer durationSeconds, Integer distanceMeters, Integer transfers,
                 Integer fare, Integer taxiFare, String summary, List<double[]> path, String landingUrl) {

        public static Route of(RouteStatus status, String landingUrl) {
            return new Route(status, null, null, null, null, null, null, List.of(), landingUrl);
        }
    }
}
