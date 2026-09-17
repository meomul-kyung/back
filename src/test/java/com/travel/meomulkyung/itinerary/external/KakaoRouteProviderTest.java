package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class KakaoRouteProviderTest {

    private static final RouteProvider.Point DOWNTOWN = new RouteProvider.Point(36.5654, 128.7236);
    private static final RouteProvider.Point HAHOE = new RouteProvider.Point(36.5390, 128.5180);

    private MockRestServiceServer server;

    private KakaoRouteProvider provider(String key) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        KakaoRouteProperties properties = new KakaoRouteProperties();
        properties.setRestApiKey(key);
        properties.setMobilityBaseUrl("https://navi.example");
        properties.setMapBaseUrl("https://map.example");
        return new KakaoRouteProvider(builder.build(), new ObjectMapper(), properties);
    }

    @Test
    void carSendsKakaoAkHeaderWithLongitudeLatitudeOrderAndParsesSummaryAndVertexes() {
        KakaoRouteProvider provider = provider("test-key");
        server.expect(requestTo(startsWith("https://navi.example/v1/directions")))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andExpect(queryParam("origin", "128.7236,36.5654"))
                .andExpect(queryParam("destination", "128.518,36.539"))
                .andRespond(withSuccess("""
                        {"routes":[{"result_code":0,"result_msg":"길찾기 성공",
                          "summary":{"distance":26500,"duration":2460,"fare":{"taxi":25600,"toll":0}},
                          "sections":[{"roads":[
                            {"vertexes":[128.7236,36.5654,128.7000,36.5600]},
                            {"vertexes":[128.7000,36.5600,128.5180,36.5390]}]}]}]}
                        """, MediaType.APPLICATION_JSON));

        RouteProvider.Route route = provider.car(DOWNTOWN, HAHOE);

        server.verify();
        assertThat(route.status()).isEqualTo(RouteProvider.RouteStatus.OK);
        assertThat(route.durationSeconds()).isEqualTo(2460);
        assertThat(route.distanceMeters()).isEqualTo(26500);
        assertThat(route.taxiFare()).isEqualTo(25600);
        assertThat(route.fare()).isNull();
        // 연속 중복 좌표는 한 번만, [위도, 경도] 순서
        assertThat(route.path()).hasSize(3);
        assertThat(route.path().get(0)).containsExactly(36.5654, 128.7236);
        assertThat(route.path().get(2)).containsExactly(36.5390, 128.5180);
        assertThat(route.landingUrl()).startsWith("https://map.kakao.com/link/by/car/");
    }

    @Test
    void carWithNonZeroResultCodeIsNoRoute() {
        KakaoRouteProvider provider = provider("test-key");
        server.expect(requestTo(startsWith("https://navi.example/v1/directions")))
                .andRespond(withSuccess("""
                        {"routes":[{"result_code":104,"result_msg":"경로 탐색 불가"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.car(DOWNTOWN, HAHOE).status()).isEqualTo(RouteProvider.RouteStatus.NO_ROUTE);
    }

    @Test
    void transitPrefersDirectRouteWithinTenMinutesOfFastest() {
        KakaoRouteProvider provider = provider("test-key");
        server.expect(requestTo(startsWith("https://map.example/v2/routing/publictraffic")))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andExpect(queryParam("start_x", "128.7236"))
                .andExpect(queryParam("start_y", "36.5654"))
                .andRespond(withSuccess(transitJson(3820), MediaType.APPLICATION_JSON));

        RouteProvider.Route route = provider.transit(DOWNTOWN, HAHOE);

        assertThat(route.status()).isEqualTo(RouteProvider.RouteStatus.OK);
        assertThat(route.durationSeconds()).isEqualTo(3820);
        assertThat(route.transfers()).isZero();
        assertThat(route.fare()).isEqualTo(1400);
        // 도보 구간은 요약에서 빠진다
        assertThat(route.summary()).isEqualTo("간선 210 (신시장 > 하회마을)");
        assertThat(route.path()).isNotEmpty();
        assertThat(route.landingUrl()).isEqualTo("https://map.kakao.com/link/by/traffic/from-api");
    }

    @Test
    void transitFallsBackToFastestWhenDirectRouteIsTooSlow() {
        KakaoRouteProvider provider = provider("test-key");
        server.expect(requestTo(startsWith("https://map.example/v2/routing/publictraffic")))
                .andRespond(withSuccess(transitJson(4000), MediaType.APPLICATION_JSON));

        RouteProvider.Route route = provider.transit(DOWNTOWN, HAHOE);

        assertThat(route.durationSeconds()).isEqualTo(3300);
        assertThat(route.transfers()).isEqualTo(1);
        assertThat(route.summary()).isEqualTo("급행 급행1 (신시장 > 경상북도교육청) → 지선 풍천2 (경상북도교육청 > 하회마을)");
    }

    @Test
    void transitWithoutNearbyStopIsNoStop() {
        KakaoRouteProvider provider = provider("test-key");
        server.expect(requestTo(startsWith("https://map.example/v2/routing/publictraffic")))
                .andRespond(withSuccess("""
                        {"status":"ENDNODES_NULL","properties":{"landingURL":"https://map.kakao.com/link/by/traffic/x"}}
                        """, MediaType.APPLICATION_JSON));

        RouteProvider.Route route = provider.transit(DOWNTOWN, HAHOE);

        assertThat(route.status()).isEqualTo(RouteProvider.RouteStatus.NO_STOP);
        assertThat(route.landingUrl()).isEqualTo("https://map.kakao.com/link/by/traffic/x");
    }

    @Test
    void transitNoResultsIsNoRoute() {
        KakaoRouteProvider provider = provider("test-key");
        server.expect(requestTo(startsWith("https://map.example/v2/routing/publictraffic")))
                .andRespond(withSuccess("{\"status\":\"NO_RESULTS\"}", MediaType.APPLICATION_JSON));

        assertThat(provider.transit(DOWNTOWN, HAHOE).status()).isEqualTo(RouteProvider.RouteStatus.NO_ROUTE);
    }

    @Test
    void nearbyPointsDoNotCallTheApi() {
        KakaoRouteProvider provider = provider("test-key");
        RouteProvider.Point next = new RouteProvider.Point(36.5660, 128.7240);

        assertThat(provider.car(DOWNTOWN, next).status()).isEqualTo(RouteProvider.RouteStatus.NEARBY);
        assertThat(provider.transit(DOWNTOWN, next).status()).isEqualTo(RouteProvider.RouteStatus.NEARBY);
        server.verify();
    }

    @Test
    void missingKeyIsUnavailableWithoutCallingTheApi() {
        KakaoRouteProvider provider = provider("");

        assertThat(provider.car(DOWNTOWN, HAHOE).status()).isEqualTo(RouteProvider.RouteStatus.UNAVAILABLE);
        assertThat(provider.transit(DOWNTOWN, HAHOE).status()).isEqualTo(RouteProvider.RouteStatus.UNAVAILABLE);
        server.verify();
    }

    @Test
    void httpErrorIsUnavailable() {
        KakaoRouteProvider provider = provider("wrong-key");
        server.expect(requestTo(startsWith("https://navi.example/v1/directions")))
                .andRespond(withUnauthorizedRequest());

        RouteProvider.Route route = provider.car(DOWNTOWN, HAHOE);

        assertThat(route.status()).isEqualTo(RouteProvider.RouteStatus.UNAVAILABLE);
        assertThat(route.landingUrl()).isNotBlank();
    }

    @Test
    void distanceBetweenDowntownAndHahoeIsAboutEighteenKilometers() {
        assertThat(KakaoRouteProvider.distanceMeters(DOWNTOWN, HAHOE)).isBetween(18_000.0, 19_500.0);
    }

    /** 최단(환승 1회, 3300초) + 직행(directTime초) + 비현실 경로(9700초) */
    private String transitJson(int directTime) {
        return """
                {"status":"OK","properties":{"landingURL":"https://map.kakao.com/link/by/traffic/from-api"},
                 "routes":[
                  {"properties":{"type":"BUS","totalDistance":28081,"totalTime":3300,"transfers":1,"fare":{"value":1400}},
                   "steps":[
                    {"properties":{"type":"BUS","guidance":"급행 급행1 (신시장 > 경상북도교육청)"},"path":{"points":[[128.7238,36.5634],[128.5115,36.5773]]}},
                    {"properties":{"type":"WALKING","guidance":"도보 이동"},"path":{"points":[[128.5115,36.5773],[128.5131,36.5777]]}},
                    {"properties":{"type":"BUS","guidance":"지선 풍천2 (경상북도교육청 > 하회마을)"},"path":{"points":[[128.5131,36.5777],[128.5197,36.5393]]}}]},
                  {"properties":{"type":"BUS","totalDistance":25975,"totalTime":%d,"transfers":0,"fare":{"value":1400}},
                   "steps":[
                    {"properties":{"type":"BUS","guidance":" 간선 210 (신시장 > 하회마을)"},"path":{"points":[[128.7238,36.5634],[128.5197,36.5393]]}}]},
                  {"properties":{"type":"BUS","totalDistance":32811,"totalTime":9700,"transfers":0,"fare":{"value":1400}},"steps":[]}
                 ]}
                """.formatted(directTime);
    }
}
