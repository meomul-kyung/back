package com.travel.meomulkyung.itinerary.service;

import com.travel.meomulkyung.itinerary.domain.Itinerary;
import com.travel.meomulkyung.itinerary.domain.ItineraryItem;
import com.travel.meomulkyung.itinerary.domain.TransportMode;
import com.travel.meomulkyung.itinerary.dto.ItineraryRouteResponses;
import com.travel.meomulkyung.itinerary.external.RegionTransitProperties;
import com.travel.meomulkyung.itinerary.external.RouteProvider;
import com.travel.meomulkyung.itinerary.repository.ItineraryRepository;
import com.travel.meomulkyung.region.domain.Region;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 일정 하루치 장소 사이의 이동정보를 조회한다.
 *
 * <p>결과는 저장하지 않고 요청마다 외부 API를 호출한다.
 * 외부 호출이 느릴 수 있어 DB 트랜잭션 밖에서 호출한다. (일정은 fetch join으로 한 번에 읽는다)
 */
@Service
@RequiredArgsConstructor
public class ItineraryRouteService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    static final String CAR_NOTICE = "조회 시점 교통 상황 기준 예상 소요시간입니다.";
    static final String TRANSIT_NOTICE = "배차 대기시간은 포함되지 않습니다. 탑승 전 카카오맵에서 실시간 운행 정보를 확인하세요.";
    static final String SOURCE_CAR = "카카오모빌리티";
    static final String SOURCE_TRANSIT = "카카오맵";

    private final ItineraryRepository itineraries;
    private final RouteProvider routeProvider;
    private final Clock clock;
    private final RegionTransitProperties regionTransitProperties;

    public ItineraryRouteResponses.DayRoutes routes(Long userId, Long itineraryId, int dayNumber, String modeValue) {
        TransportMode mode = TransportMode.from(modeValue).orElseThrow(() ->
                new ItineraryException(HttpStatus.BAD_REQUEST, "INVALID_TRANSPORT_MODE", "이동수단은 CAR 또는 TRANSIT만 가능합니다."));
        Itinerary itinerary = itineraries.findDetailById(itineraryId).orElseThrow(() ->
                new ItineraryException(HttpStatus.NOT_FOUND, "ITINERARY_NOT_FOUND", "일정을 찾을 수 없습니다."));
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new ItineraryException(HttpStatus.FORBIDDEN, "ITINERARY_ACCESS_DENIED", "일정 접근 권한이 없습니다.");
        }
        int days = itinerary.getNights() + 1;
        if (dayNumber < 1 || dayNumber > days) {
            throw new ItineraryException(HttpStatus.NOT_FOUND, "ITINERARY_DAY_NOT_FOUND", "해당 일차를 찾을 수 없습니다.");
        }
        List<ItineraryItem> dayItems = itinerary.getItems().stream()
                .filter(item -> item.getDayNumber() == dayNumber)
                .toList();
        return new ItineraryRouteResponses.DayRoutes(
                itinerary.getId(),
                dayNumber,
                itinerary.getStartDate().plusDays(dayNumber - 1L),
                mode.name(),
                segments(dayItems, mode),
                mode == TransportMode.CAR ? CAR_NOTICE : TRANSIT_NOTICE,
                mode == TransportMode.CAR ? SOURCE_CAR : SOURCE_TRANSIT,
                regionTransit(itinerary.getRegion(), mode),
                OffsetDateTime.now(clock.withZone(SEOUL)));
    }

    /**
     * 지역 버스 안내를 설정에서 채운다. 외부 호출은 없다.
     *
     * <p>자동차 조회에는 의미가 없어 null을 내리고, 설정에 아무것도 없는 지역도 null이다.
     * 지역이 무료여도 구간 요금({@code Segment.fare})은 실측값을 그대로 둔다.
     * 시·군을 넘는 노선은 유료일 수 있어 둘이 달라도 모순이 아니다.
     */
    ItineraryRouteResponses.RegionTransit regionTransit(Region region, TransportMode mode) {
        if (mode != TransportMode.TRANSIT || region == null) {
            return null;
        }
        Long regionId = region.getId();
        String url = regionTransitProperties.timetableUrl(regionId);
        ItineraryRouteResponses.Timetable timetable = url == null || url.isBlank()
                ? null
                : new ItineraryRouteResponses.Timetable(url, regionTransitProperties.timetableSource(regionId));
        ItineraryRouteResponses.FreeBus freeBus = regionTransitProperties.isFreeBusRegion(regionId)
                ? new ItineraryRouteResponses.FreeBus(regionTransitProperties.getFreeBusLabel(),
                        regionTransitProperties.getFreeBusCaution(), regionTransitProperties.getFreeBusBasis())
                : null;
        if (timetable == null && freeBus == null) {
            return null;
        }
        return new ItineraryRouteResponses.RegionTransit(timetable, freeBus, regionTransitProperties.getCheckedOn());
    }

    /** 좌표가 있는 장소만 방문 순서대로 이어 구간을 만든다. (도착·휴식·출발, 좌표 없는 기존 항목은 건너뜀) */
    List<ItineraryRouteResponses.Segment> segments(List<ItineraryItem> dayItems, TransportMode mode) {
        List<ItineraryItem> stops = dayItems.stream()
                .filter(item -> item.getLatitude() != null && item.getLongitude() != null)
                .sorted(Comparator.comparingInt(ItineraryItem::getSequence))
                .toList();
        List<ItineraryRouteResponses.Segment> segments = new ArrayList<>();
        for (int i = 0; i + 1 < stops.size(); i++) {
            ItineraryItem from = stops.get(i);
            ItineraryItem to = stops.get(i + 1);
            RouteProvider.Point start = new RouteProvider.Point(from.getLatitude(), from.getLongitude());
            RouteProvider.Point end = new RouteProvider.Point(to.getLatitude(), to.getLongitude());
            RouteProvider.Route route = mode == TransportMode.CAR
                    ? routeProvider.car(start, end)
                    : routeProvider.transit(start, end);
            segments.add(segment(from, to, mode, route));
        }
        return List.copyOf(segments);
    }

    private ItineraryRouteResponses.Segment segment(ItineraryItem from, ItineraryItem to, TransportMode mode,
                                                    RouteProvider.Route route) {
        Integer minutes = route.durationSeconds() == null ? null : (int) Math.ceil(route.durationSeconds() / 60.0);
        Boolean free = mode == TransportMode.TRANSIT && route.fare() != null ? route.fare() == 0 : null;
        return new ItineraryRouteResponses.Segment(
                from.getId(), from.getCustomTitle(),
                to.getId(), to.getCustomTitle(),
                route.status().name(),
                minutes,
                route.distanceMeters(),
                route.transfers(),
                route.fare(),
                free,
                route.taxiFare(),
                route.summary(),
                route.path(),
                route.landingUrl());
    }
}
