package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 카카오 경로 조회 구현.
 *
 * <ul>
 *   <li>자동차: 카카오모빌리티 {@code GET /v1/directions}</li>
 *   <li>대중교통: 카카오맵 {@code GET /v2/routing/publictraffic}</li>
 * </ul>
 *
 * <p>대중교통 API는 출발 시각을 받지 않으므로 소요시간에 배차 대기시간이 포함되지 않는다.
 * 응답이 소요시간순으로 정렬되어 있지 않고 비현실적인 경로가 섞여 오므로 {@link #selectTransitRoute}에서 직접 고른다.
 */
public class KakaoRouteProvider implements RouteProvider {

    private static final Logger log = LoggerFactory.getLogger(KakaoRouteProvider.class);
    /** 최단 소요시간의 이 배수를 넘는 대중교통 경로는 후보에서 뺀다. */
    private static final double MAX_DURATION_RATIO = 2.0;
    /** 환승 없는 경로가 최단보다 이 시간(초) 이내로 느리면 환승 없는 경로를 고른다. */
    private static final int DIRECT_ROUTE_TOLERANCE_SECONDS = 600;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KakaoRouteProperties properties;

    public KakaoRouteProvider(RestClient restClient, ObjectMapper objectMapper, KakaoRouteProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Route car(Point from, Point to) {
        String landingUrl = landingUrl("car", from, to);
        if (isNearby(from, to)) {
            return Route.of(RouteStatus.NEARBY, landingUrl);
        }
        URI uri = UriComponentsBuilder.fromUriString(properties.getMobilityBaseUrl())
                .path("/v1/directions")
                .queryParam("origin", xy(from))
                .queryParam("destination", xy(to))
                .queryParam("priority", "RECOMMEND")
                .queryParam("summary", "false")
                .build()
                .encode()
                .toUri();
        JsonNode root = call(uri, "car");
        if (root == null) {
            return Route.of(RouteStatus.UNAVAILABLE, landingUrl);
        }
        JsonNode route = root.path("routes").path(0);
        if (route.isMissingNode() || route.path("result_code").asInt(-1) != 0) {
            return Route.of(RouteStatus.NO_ROUTE, landingUrl);
        }
        JsonNode summary = route.path("summary");
        List<double[]> path = new ArrayList<>();
        for (JsonNode section : route.path("sections")) {
            for (JsonNode road : section.path("roads")) {
                JsonNode vertexes = road.path("vertexes");
                for (int i = 0; i + 1 < vertexes.size(); i += 2) {
                    addPoint(path, vertexes.get(i + 1).asDouble(), vertexes.get(i).asDouble());
                }
            }
        }
        return new Route(RouteStatus.OK,
                intOrNull(summary.path("duration")),
                intOrNull(summary.path("distance")),
                null,
                null,
                intOrNull(summary.path("fare").path("taxi")),
                null,
                List.copyOf(path),
                landingUrl);
    }

    @Override
    public Route transit(Point from, Point to) {
        String fallbackLandingUrl = landingUrl("traffic", from, to);
        if (isNearby(from, to)) {
            return Route.of(RouteStatus.NEARBY, fallbackLandingUrl);
        }
        URI uri = UriComponentsBuilder.fromUriString(properties.getMapBaseUrl())
                .path("/v2/routing/publictraffic")
                .queryParam("start_x", from.longitude())
                .queryParam("start_y", from.latitude())
                .queryParam("end_x", to.longitude())
                .queryParam("end_y", to.latitude())
                .build()
                .encode()
                .toUri();
        JsonNode root = call(uri, "transit");
        if (root == null) {
            return Route.of(RouteStatus.UNAVAILABLE, fallbackLandingUrl);
        }
        String landingUrl = text(root.path("properties").path("landingURL"), fallbackLandingUrl);
        String status = root.path("status").asText("");
        switch (status) {
            case "OK" -> { }
            case "STARTNODES_NULL", "ENDNODES_NULL" -> { return Route.of(RouteStatus.NO_STOP, landingUrl); }
            default -> { return Route.of(RouteStatus.NO_ROUTE, landingUrl); }
        }
        JsonNode selected = selectTransitRoute(root.path("routes"));
        if (selected == null) {
            return Route.of(RouteStatus.NO_ROUTE, landingUrl);
        }
        JsonNode summary = selected.path("properties");
        List<String> legs = new ArrayList<>();
        List<double[]> path = new ArrayList<>();
        for (JsonNode step : selected.path("steps")) {
            JsonNode stepProperties = step.path("properties");
            if (!"WALKING".equals(stepProperties.path("type").asText())) {
                String guidance = text(stepProperties.path("guidance"), null);
                if (guidance != null) {
                    legs.add(guidance.trim());
                }
            }
            for (JsonNode point : step.path("path").path("points")) {
                if (point.size() >= 2) {
                    addPoint(path, point.get(1).asDouble(), point.get(0).asDouble());
                }
            }
        }
        return new Route(RouteStatus.OK,
                intOrNull(summary.path("totalTime")),
                intOrNull(summary.path("totalDistance")),
                intOrNull(summary.path("transfers")),
                intOrNull(summary.path("fare").path("value")),
                null,
                legs.isEmpty() ? null : String.join(" → ", legs),
                List.copyOf(path),
                landingUrl);
    }

    /**
     * 최단 소요시간의 2배를 넘는 경로를 버리고,
     * 환승 없는 경로가 최단보다 10분 이내로 느리면 그 경로를, 아니면 최단 경로를 고른다.
     */
    static JsonNode selectTransitRoute(JsonNode routes) {
        List<JsonNode> candidates = new ArrayList<>();
        routes.forEach(route -> {
            if (route.path("properties").path("totalTime").canConvertToInt()) {
                candidates.add(route);
            }
        });
        if (candidates.isEmpty()) {
            return null;
        }
        Comparator<JsonNode> byTime = Comparator.comparingInt(route -> totalTime(route));
        JsonNode fastest = candidates.stream().min(byTime).orElseThrow();
        int fastestTime = totalTime(fastest);
        List<JsonNode> reasonable = candidates.stream()
                .filter(route -> totalTime(route) <= fastestTime * MAX_DURATION_RATIO)
                .toList();
        return reasonable.stream()
                .filter(route -> route.path("properties").path("transfers").asInt(Integer.MAX_VALUE) == 0)
                .filter(route -> totalTime(route) <= fastestTime + DIRECT_ROUTE_TOLERANCE_SECONDS)
                .min(byTime)
                .orElse(fastest);
    }

    private static int totalTime(JsonNode route) {
        return route.path("properties").path("totalTime").asInt();
    }

    private JsonNode call(URI uri, String kind) {
        if (!StringUtils.hasText(properties.getRestApiKey())) {
            log.warn("kakao_route_unavailable kind={} reason=missing_key", kind);
            return null;
        }
        try {
            String body = restClient.get()
                    .uri(uri)
                    .header("Authorization", "KakaoAK " + properties.getRestApiKey())
                    .retrieve()
                    .body(String.class);
            return body == null ? null : objectMapper.readTree(body);
        } catch (RestClientResponseException exception) {
            log.warn("kakao_route_unavailable kind={} httpStatus={}", kind, exception.getStatusCode().value());
            return null;
        } catch (RestClientException | JsonProcessingException exception) {
            log.warn("kakao_route_unavailable kind={} causeClass={}", kind, exception.getClass().getSimpleName());
            return null;
        }
    }

    private boolean isNearby(Point from, Point to) {
        return distanceMeters(from, to) < properties.getNearbyDistanceMeters();
    }

    static double distanceMeters(Point from, Point to) {
        double lat1 = Math.toRadians(from.latitude());
        double lat2 = Math.toRadians(to.latitude());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(to.longitude() - from.longitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static String xy(Point point) {
        return point.longitude() + "," + point.latitude();
    }

    /** 카카오맵 길찾기 링크. 대중교통 API 응답의 landingURL과 같은 형식이다. */
    private static String landingUrl(String mode, Point from, Point to) {
        return "https://map.kakao.com/link/by/" + mode + "/"
                + UriUtils.encodePathSegment("출발", StandardCharsets.UTF_8) + "," + coordinate(from) + "/"
                + UriUtils.encodePathSegment("도착", StandardCharsets.UTF_8) + "," + coordinate(to);
    }

    private static String coordinate(Point point) {
        return String.format(Locale.ROOT, "%.6f,%.6f", point.latitude(), point.longitude());
    }

    /** 연속으로 같은 좌표가 오면 한 번만 넣어 응답 크기를 줄인다. */
    private static void addPoint(List<double[]> path, double latitude, double longitude) {
        if (!path.isEmpty()) {
            double[] last = path.get(path.size() - 1);
            if (last[0] == latitude && last[1] == longitude) {
                return;
            }
        }
        path.add(new double[]{latitude, longitude});
    }

    private static Integer intOrNull(JsonNode node) {
        return node.canConvertToInt() || node.isTextual() && node.asText().matches("-?\\d+") ? node.asInt() : null;
    }

    private static String text(JsonNode node, String fallback) {
        String value = node.asText(null);
        return StringUtils.hasText(value) ? value : fallback;
    }
}
