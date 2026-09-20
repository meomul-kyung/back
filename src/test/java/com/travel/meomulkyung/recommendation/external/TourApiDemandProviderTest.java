package com.travel.meomulkyung.recommendation.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TourApiDemandProviderTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private TourDemandProperties properties;
    private TourApiDemandProvider provider;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        properties = new TourDemandProperties();
        properties.setBaseUrl("https://demand.test/B551011/AreaTarDemDsService");
        properties.setServiceKey("TEST%2BKEY");
        properties.setBaseYm("202608");
        properties.setAreaCd("47");
        properties.getSignguCodes().put(1L, "47170");
        properties.getSignguCodes().put(12L, "47770");
        provider = new TourApiDemandProvider(builder.build(), new ObjectMapper(), properties);
    }

    @Test
    @DisplayName("숙박일수 지표 세 개를 지역별 체류 강도로 합친다")
    void combinesThreeIndicatorsIntoStayProfiles() {
        expectIndicator("2103", body("2103", "71.35", "75.34"));
        expectIndicator("2104", body("2104", "74.35", "79.28"));
        expectIndicator("2105", body("2105", "77.41", "65.21"));

        var profiles = provider.findStayProfiles();

        assertThat(profiles).containsOnlyKeys(1L, 12L);
        assertThat(profiles.get(1L)).isEqualTo(new TourDemandProvider.StayProfile(71.35, 74.35, 77.41));
        assertThat(profiles.get(12L).valueFor(1)).isEqualTo(75.34);
        assertThat(profiles.get(12L).valueFor(2)).isEqualTo(79.28);
        // API는 3박 이상을 하나로 묶으므로 4박 이상도 같은 값을 쓴다.
        assertThat(profiles.get(12L).valueFor(5)).isEqualTo(65.21);
        server.verify();
    }

    @Test
    @DisplayName("지역 수와 무관하게 지표당 한 번만 호출하고 결과를 캐시한다")
    void callsOncePerIndicatorAndCachesResult() {
        expectIndicator("2103", body("2103", "71.35", "75.34"));
        expectIndicator("2104", body("2104", "74.35", "79.28"));
        expectIndicator("2105", body("2105", "77.41", "65.21"));

        provider.findStayProfiles();
        provider.findStayProfiles();

        // 기대한 요청이 세 건뿐이므로, 두 번째 호출이 네트워크를 탔다면 verify 가 실패한다.
        server.verify();
    }

    @Test
    @DisplayName("도 전체 집계 행은 시군구와 산식이 달라 제외한다")
    void ignoresProvinceWideRow() {
        String withProvinceRow = """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                "body":{"items":{"item":[
                {"baseYm":"202608","areaCd":"47","signguCd":"0","tarSjrnDsIxCd":"2103","tarSjrnDsIxVal":"99.99"},
                {"baseYm":"202608","areaCd":"47","signguCd":"47170","tarSjrnDsIxCd":"2103","tarSjrnDsIxVal":"71.35"},
                {"baseYm":"202608","areaCd":"47","signguCd":"47770","tarSjrnDsIxCd":"2103","tarSjrnDsIxVal":"75.34"}]}}}}
                """;
        expectIndicator("2103", withProvinceRow);
        expectIndicator("2104", body("2104", "74.35", "79.28"));
        expectIndicator("2105", body("2105", "77.41", "65.21"));

        var profiles = provider.findStayProfiles();

        assertThat(profiles).containsOnlyKeys(1L, 12L);
        assertThat(profiles.values()).noneMatch(profile -> profile.oneNight() == 99.99);
        server.verify();
    }

    @Test
    @DisplayName("지표 하나라도 빠지면 그 지역은 제외해 정규화 기준을 흐리지 않는다")
    void dropsRegionWhenAnyIndicatorIsMissing() {
        expectIndicator("2103", body("2103", "71.35", "75.34"));
        expectIndicator("2104", """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                "body":{"items":{"item":[
                {"signguCd":"47170","tarSjrnDsIxCd":"2104","tarSjrnDsIxVal":"74.35"}]}}}}
                """);
        expectIndicator("2105", body("2105", "77.41", "65.21"));

        var profiles = provider.findStayProfiles();

        assertThat(profiles).containsOnlyKeys(1L);
        server.verify();
    }

    @Test
    @DisplayName("호출이 실패해도 예외를 던지지 않고 빈 결과를 돌려준다")
    void serverErrorDoesNotPropagate() {
        server.expect(requestTo(containsString("tarSjrnDsIxCd=2103"))).andRespond(withServerError());
        server.expect(requestTo(containsString("tarSjrnDsIxCd=2104"))).andRespond(withServerError());
        server.expect(requestTo(containsString("tarSjrnDsIxCd=2105"))).andRespond(withServerError());

        assertThat(provider.findStayProfiles()).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("기상청·관광공사 오류 코드가 오면 빈 결과를 돌려준다")
    void applicationErrorCodeIsEmpty() {
        String error = """
                {"response":{"header":{"resultCode":"03","resultMsg":"NODATA_ERROR"}}}
                """;
        expectIndicator("2103", error);
        expectIndicator("2104", error);
        expectIndicator("2105", error);

        assertThat(provider.findStayProfiles()).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("인증키나 시군구 매핑이 없으면 호출하지 않는다")
    void skipsCallWhenNotConfigured() {
        properties.setServiceKey("");

        assertThat(provider.findStayProfiles()).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("Encoding 인증키는 이중 인코딩하지 않고 그대로 붙인다")
    void appendsEncodedServiceKeyAsIs() {
        server.expect(requestTo(containsString("serviceKey=TEST%2BKEY")))
                .andRespond(withSuccess(body("2103", "71.35", "75.34"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("serviceKey=TEST%2BKEY")))
                .andRespond(withSuccess(body("2104", "74.35", "79.28"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("serviceKey=TEST%2BKEY")))
                .andRespond(withSuccess(body("2105", "77.41", "65.21"), MediaType.APPLICATION_JSON));

        assertThat(provider.findStayProfiles()).isNotEmpty();
        server.verify();
    }

    private void expectIndicator(String indicatorCode, String responseBody) {
        server.expect(requestTo(allOf(
                        containsString("tarSjrnDsIxCd=" + indicatorCode),
                        containsString("areaCd=47"),
                        containsString("baseYm=202608"))))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private String body(String indicatorCode, String andong, String yeongdeok) {
        return """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                "body":{"items":{"item":[
                {"baseYm":"202608","areaCd":"47","signguCd":"47170","tarSjrnDsIxCd":"%s","tarSjrnDsIxVal":"%s"},
                {"baseYm":"202608","areaCd":"47","signguCd":"47770","tarSjrnDsIxCd":"%s","tarSjrnDsIxVal":"%s"}]}}}}
                """.formatted(indicatorCode, andong, indicatorCode, yeongdeok);
    }
}
