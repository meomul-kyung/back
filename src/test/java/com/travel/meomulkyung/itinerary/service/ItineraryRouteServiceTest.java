package com.travel.meomulkyung.itinerary.service;

import com.travel.meomulkyung.itinerary.domain.ItineraryItem;
import com.travel.meomulkyung.itinerary.domain.ItineraryItemType;
import com.travel.meomulkyung.itinerary.domain.TransportMode;
import com.travel.meomulkyung.itinerary.dto.ItineraryRouteResponses;
import com.travel.meomulkyung.itinerary.external.RouteProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItineraryRouteServiceTest {

    private final RecordingRouteProvider provider = new RecordingRouteProvider();
    private final ItineraryRouteService service = new ItineraryRouteService(null, provider, Clock.systemUTC());

    @Test
    void connectsOnlyItemsWithCoordinatesInVisitOrder() {
        List<ItineraryItem> items = List.of(
                place(4, "식당", 36.54, 128.52),
                new ItineraryItem(null, 1, 1, ItineraryItemType.ARRIVAL, null, "ARRIVAL"),
                place(2, "관광지A", 36.56, 128.72),
                new ItineraryItem(null, 1, 3, ItineraryItemType.TOURIST_SPOT, 9L, "좌표 없는 기존 항목"),
                place(5, "관광지B", 36.60, 128.60));

        List<ItineraryRouteResponses.Segment> segments = service.segments(items, TransportMode.CAR);

        assertThat(segments).extracting(ItineraryRouteResponses.Segment::fromTitle).containsExactly("관광지A", "식당");
        assertThat(segments).extracting(ItineraryRouteResponses.Segment::toTitle).containsExactly("식당", "관광지B");
        assertThat(provider.carCalls).isEqualTo(2);
        assertThat(provider.transitCalls).isZero();
    }

    @Test
    void transitRoundsSecondsUpToMinutesAndMarksFreeFare() {
        provider.next = new RouteProvider.Route(RouteProvider.RouteStatus.OK, 3801, 25975, 0, 0, null,
                "농어촌 203 (약수탕 > 월외리)", List.of(), "https://map.kakao.com/link/by/traffic/x");

        List<ItineraryRouteResponses.Segment> segments = service.segments(
                List.of(place(2, "달기약수탕", 36.46, 129.06), place(3, "달기폭포", 36.47, 129.10)), TransportMode.TRANSIT);

        ItineraryRouteResponses.Segment segment = segments.get(0);
        assertThat(provider.transitCalls).isEqualTo(1);
        assertThat(segment.status()).isEqualTo("OK");
        assertThat(segment.durationMinutes()).isEqualTo(64);
        assertThat(segment.fare()).isZero();
        assertThat(segment.free()).isTrue();
        assertThat(segment.summary()).isEqualTo("농어촌 203 (약수탕 > 월외리)");
    }

    @Test
    void carDoesNotReportFreeFlagAndKeepsFailureStatusPerSegment() {
        provider.next = RouteProvider.Route.of(RouteProvider.RouteStatus.UNAVAILABLE, "https://map.kakao.com/link/by/car/x");

        ItineraryRouteResponses.Segment segment = service.segments(
                List.of(place(2, "A", 36.56, 128.72), place(3, "B", 36.54, 128.52)), TransportMode.CAR).get(0);

        assertThat(segment.status()).isEqualTo("UNAVAILABLE");
        assertThat(segment.durationMinutes()).isNull();
        assertThat(segment.free()).isNull();
        assertThat(segment.landingUrl()).isNotBlank();
    }

    @Test
    void singleStopDayHasNoSegments() {
        assertThat(service.segments(List.of(place(2, "A", 36.56, 128.72)), TransportMode.CAR)).isEmpty();
        assertThat(provider.carCalls).isZero();
    }

    @Test
    void invalidModeIsRejectedBeforeLoadingTheItinerary() {
        assertThatThrownBy(() -> service.routes(1L, 1L, 1, "WALK"))
                .isInstanceOf(ItineraryException.class)
                .extracting("code").isEqualTo("INVALID_TRANSPORT_MODE");
    }

    @Test
    void transportModeDefaultsToCarAndIgnoresCase() {
        assertThat(TransportMode.from(null)).contains(TransportMode.CAR);
        assertThat(TransportMode.from("transit")).contains(TransportMode.TRANSIT);
        assertThat(TransportMode.from("bike")).isEmpty();
    }

    private static ItineraryItem place(int sequence, String title, double latitude, double longitude) {
        return new ItineraryItem(null, 1, sequence, ItineraryItemType.TOURIST_SPOT, (long) sequence, title,
                null, null, latitude, longitude);
    }

    private static class RecordingRouteProvider implements RouteProvider {
        int carCalls;
        int transitCalls;
        Route next = new Route(RouteStatus.OK, 2460, 26500, null, null, 25600, null, new ArrayList<>(), "https://x");

        @Override
        public Route car(Point from, Point to) {
            carCalls++;
            return next;
        }

        @Override
        public Route transit(Point from, Point to) {
            transitCalls++;
            return next;
        }
    }
}
