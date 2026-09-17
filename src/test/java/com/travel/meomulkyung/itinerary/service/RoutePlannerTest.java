package com.travel.meomulkyung.itinerary.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class RoutePlannerTest {

    private static final Function<Spot, RoutePlanner.Point> LOCATOR =
            spot -> RoutePlanner.point(spot.latitude(), spot.longitude());

    @Test
    void chainsNearbySpotsTogetherEvenWhenTheInputIsInterleaved() {
        List<Spot> mixed = List.of(
                new Spot("서쪽1", 36.50, 128.50),
                new Spot("동쪽1", 36.95, 129.40),
                new Spot("서쪽2", 36.52, 128.52),
                new Spot("동쪽2", 36.97, 129.42),
                new Spot("서쪽3", 36.54, 128.54),
                new Spot("동쪽3", 36.99, 129.44));

        List<String> ordered = RoutePlanner.orderByProximity(mixed, LOCATOR).stream().map(Spot::name).toList();

        assertThat(ordered.subList(0, 3)).allMatch(name -> name.startsWith(ordered.get(0).substring(0, 2)));
        assertThat(ordered.subList(3, 6)).allMatch(name -> name.startsWith(ordered.get(3).substring(0, 2)));
    }

    @Test
    void spotsWithoutCoordinatesKeepTheirOrderAtTheEnd() {
        List<Spot> spots = List.of(
                new Spot("좌표없음1", null, null),
                new Spot("먼곳", 36.99, 129.44),
                new Spot("좌표없음2", 36.50, null),
                new Spot("가까운곳", 36.97, 129.42));

        List<String> ordered = RoutePlanner.orderByProximity(spots, LOCATOR).stream().map(Spot::name).toList();

        assertThat(ordered).hasSize(4);
        assertThat(ordered.subList(0, 2)).containsExactlyInAnyOrder("먼곳", "가까운곳");
        assertThat(ordered.subList(2, 4)).containsExactly("좌표없음1", "좌표없음2");
    }

    @Test
    void allWithoutCoordinatesKeepsTheOriginalOrder() {
        List<Spot> spots = List.of(new Spot("A", null, null), new Spot("B", null, null), new Spot("C", null, null));

        assertThat(RoutePlanner.orderByProximity(spots, LOCATOR).stream().map(Spot::name))
                .containsExactly("A", "B", "C");
    }

    @Test
    void nearestIndexFallsBackToTheFirstCandidateWithoutUsableCoordinates() {
        List<Spot> spots = List.of(new Spot("A", 36.50, 128.50), new Spot("B", 36.52, 128.52));

        assertThat(RoutePlanner.nearestIndex(spots, LOCATOR, null)).isZero();
        assertThat(RoutePlanner.nearestIndex(List.of(new Spot("좌표없음", null, null)), LOCATOR,
                new RoutePlanner.Point(36.5, 128.5))).isZero();
    }

    @Test
    void nearestIndexSkipsCandidatesWithoutCoordinates() {
        List<Spot> spots = List.of(
                new Spot("좌표없음", null, null),
                new Spot("먼곳", 37.90, 129.90),
                new Spot("가까운곳", 36.51, 128.51));

        assertThat(RoutePlanner.nearestIndex(spots, LOCATOR, new RoutePlanner.Point(36.50, 128.50))).isEqualTo(2);
    }

    @Test
    void midpointUsesWhicheverSideIsKnown() {
        RoutePlanner.Point first = new RoutePlanner.Point(36.0, 128.0);
        RoutePlanner.Point second = new RoutePlanner.Point(37.0, 129.0);

        assertThat(RoutePlanner.midpoint(first, second)).isEqualTo(new RoutePlanner.Point(36.5, 128.5));
        assertThat(RoutePlanner.midpoint(null, second)).isEqualTo(second);
        assertThat(RoutePlanner.midpoint(first, null)).isEqualTo(first);
        assertThat(RoutePlanner.midpoint(null, null)).isNull();
    }

    @Test
    void distanceApproximatesKilometres() {
        double oneDegreeOfLatitude = RoutePlanner.distance(
                new RoutePlanner.Point(36.0, 128.0), new RoutePlanner.Point(37.0, 128.0));

        assertThat(oneDegreeOfLatitude).isCloseTo(111.32, org.assertj.core.data.Offset.offset(0.01));
        assertThat(RoutePlanner.distance(new RoutePlanner.Point(36.0, 128.0), new RoutePlanner.Point(36.0, 129.0)))
                .isLessThan(oneDegreeOfLatitude);
    }

    private record Spot(String name, Double latitude, Double longitude) {
    }
}
