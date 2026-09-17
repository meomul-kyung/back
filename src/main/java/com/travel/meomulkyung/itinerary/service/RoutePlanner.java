package com.travel.meomulkyung.itinerary.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 저장된 좌표만으로 방문 순서를 정한다. 외부 API를 부르지 않아 쿼터를 쓰지 않는다.
 *
 * <p>한계는 분명하다. 직선거리 기준이라 산·강 때문에 실제로는 돌아가는 구간을 반영하지 못하고,
 * 최근접 이웃 방식이라 최단 경로도 보장하지 않는다. 하루 3곳 규모에서는 차이가 작다고 보고 택했다.
 *
 * <p>순수 함수만 두어 스프링 없이 단독으로 검증할 수 있게 한다.
 */
final class RoutePlanner {

    /** 위도 1도의 거리(km). 경도 1도는 위도가 높을수록 짧아져 cos로 보정한다. */
    private static final double DEGREE_KM = 111.32;

    private RoutePlanner() {
    }

    record Point(double latitude, double longitude) {
    }

    /** 좌표가 하나라도 없으면 위치를 모르는 것으로 본다. */
    static Point point(Double latitude, Double longitude) {
        return latitude == null || longitude == null ? null : new Point(latitude, longitude);
    }

    /**
     * 좌표가 있는 항목을 가까운 곳끼리 한 줄로 잇는다. 좌표 없는 항목은 원래 순서대로 뒤에 붙인다.
     *
     * <p>시작점은 <b>전체 중심에서 가장 먼 항목</b>이다. 가장자리에서 출발해야 줄이 덜 꼬이고,
     * 지역 대표 좌표 같은 새 설정값이나 의존성을 들이지 않아도 된다.
     */
    static <T> List<T> orderByProximity(List<T> values, Function<T, Point> locator) {
        List<T> located = new ArrayList<>();
        List<T> unlocated = new ArrayList<>();
        for (T value : values) {
            if (locator.apply(value) == null) {
                unlocated.add(value);
            } else {
                located.add(value);
            }
        }
        List<T> ordered = new ArrayList<>(values.size());
        if (!located.isEmpty()) {
            T current = located.remove(farthestFromCentreIndex(located, locator));
            ordered.add(current);
            while (!located.isEmpty()) {
                current = located.remove(nearestIndex(located, locator, locator.apply(current)));
                ordered.add(current);
            }
        }
        ordered.addAll(unlocated);
        return List.copyOf(ordered);
    }

    /**
     * 기준점에 가장 가까운 항목의 위치를 돌려준다.
     *
     * <p>기준점이 없거나 후보에 좌표가 없으면 첫 번째를 고른다. 즉 좌표를 모를 때는 기존 동작(목록 순)과 같다.
     */
    static <T> int nearestIndex(List<T> values, Function<T, Point> locator, Point target) {
        if (target == null) {
            return 0;
        }
        int nearest = 0;
        double shortest = Double.MAX_VALUE;
        for (int index = 0; index < values.size(); index++) {
            Point point = locator.apply(values.get(index));
            if (point == null) {
                continue;
            }
            double distance = distance(target, point);
            if (distance < shortest) {
                shortest = distance;
                nearest = index;
            }
        }
        return nearest;
    }

    /** 두 지점의 중간. 한쪽만 있으면 그 지점, 둘 다 없으면 null이다. */
    static Point midpoint(Point first, Point second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return new Point((first.latitude() + second.latitude()) / 2, (first.longitude() + second.longitude()) / 2);
    }

    /** 근사 직선거리(km). 정렬 비교용이라 지구 곡률을 엄밀하게 다루지 않는다. */
    static double distance(Point from, Point to) {
        double meanLatitude = Math.toRadians((from.latitude() + to.latitude()) / 2);
        double dx = (to.longitude() - from.longitude()) * Math.cos(meanLatitude);
        double dy = to.latitude() - from.latitude();
        return Math.sqrt(dx * dx + dy * dy) * DEGREE_KM;
    }

    private static <T> int farthestFromCentreIndex(List<T> values, Function<T, Point> locator) {
        double latitudeSum = 0;
        double longitudeSum = 0;
        for (T value : values) {
            Point point = locator.apply(value);
            latitudeSum += point.latitude();
            longitudeSum += point.longitude();
        }
        Point centre = new Point(latitudeSum / values.size(), longitudeSum / values.size());
        int farthest = 0;
        double longest = -1;
        for (int index = 0; index < values.size(); index++) {
            double distance = distance(centre, locator.apply(values.get(index)));
            if (distance > longest) {
                longest = distance;
                farthest = index;
            }
        }
        return farthest;
    }
}
