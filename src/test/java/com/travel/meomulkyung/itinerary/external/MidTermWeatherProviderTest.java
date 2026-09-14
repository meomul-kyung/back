package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.meomulkyung.region.domain.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MidTermWeatherProviderTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    /** 2026-09-10 13:00 KST → 같은 날 06시 발표분을 쓴다(4일 후 ~ 10일 후 제공). */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-10T04:00:00Z"), SEOUL);
    private static final LocalDate ANNOUNCED = LocalDate.of(2026, 9, 10);

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private MidTermWeatherProperties properties;
    private MidTermWeatherProvider provider;
    private Region region;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        properties = new MidTermWeatherProperties();
        properties.setBaseUrl("https://kma.test/1360000/MidFcstInfoService");
        properties.setServiceKey("TEST%2BKEY");
        properties.getTemperatureRegionIds().put(1L, "11H10501");
        provider = new MidTermWeatherProvider(builder.build(), new ObjectMapper(), properties, FIXED_CLOCK);
        region = new Region(1L, "안동", null, null, "identity", null);
    }

    @Test
    @DisplayName("06시 발표분으로 4일 후부터 10일 후까지 날씨와 기온을 채운다")
    void fillsForecastFromFourthToTenthDay() {
        expectTemperature();
        expectLand();

        WeatherProvider.Weather fourth = provider.weather(region, ANNOUNCED.plusDays(4));

        assertThat(fourth.available()).isTrue();
        assertThat(fourth.icon()).isEqualTo("SUNNY");
        assertThat(fourth.minimumTemperature()).isEqualTo(15);
        assertThat(fourth.maximumTemperature()).isEqualTo(26);
        // 하루를 대표하는 기온은 낮 기준이므로 최고기온을 쓴다.
        assertThat(fourth.temperature()).isEqualTo(26);
        server.verify();
    }

    @Test
    @DisplayName("오전·오후가 나뉜 날짜는 악천후 쪽을 표시한다")
    void prefersWorseOfMorningAndAfternoon() {
        expectTemperature();
        expectLand();

        // 5일 후: 오전 맑음 / 오후 흐리고 비 → 비가 이긴다
        assertThat(provider.weather(region, ANNOUNCED.plusDays(5)).icon()).isEqualTo("RAIN");
        // 6일 후: 오전 구름많고 눈 / 오후 구름많음 → 눈이 이긴다
        assertThat(provider.weather(region, ANNOUNCED.plusDays(6)).icon()).isEqualTo("SNOW");
        // 7일 후: 양쪽 모두 구름많음
        assertThat(provider.weather(region, ANNOUNCED.plusDays(7)).icon()).isEqualTo("PARTLY_CLOUDY");
        server.verify();
    }

    @Test
    @DisplayName("8일 후부터는 오전·오후 구분 없이 하루 한 값을 쓴다")
    void usesSingleValueFromEighthDay() {
        expectTemperature();
        expectLand();

        assertThat(provider.weather(region, ANNOUNCED.plusDays(8)).icon()).isEqualTo("CLOUDY");
        assertThat(provider.weather(region, ANNOUNCED.plusDays(9)).icon()).isEqualTo("SLEET");
        assertThat(provider.weather(region, ANNOUNCED.plusDays(10)).icon()).isEqualTo("SHOWER");
        server.verify();
    }

    @Test
    @DisplayName("예보 범위를 벗어난 날짜는 호출하지 않고 available=false를 돌려준다")
    void skipsCallOutsideForecastRange() {
        // 3일 후는 단기예보 담당, 11일 후는 중기예보에도 없다
        assertThat(provider.weather(region, ANNOUNCED.plusDays(3)).available()).isFalse();
        assertThat(provider.weather(region, ANNOUNCED.plusDays(11)).available()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("구역코드가 없는 지역은 호출하지 않는다")
    void skipsCallWhenRegionIsNotMapped() {
        Region unmapped = new Region(99L, "미매핑", null, null, "identity", null);

        assertThat(provider.weather(unmapped, ANNOUNCED.plusDays(4)).available()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("Encoding 인증키는 이중 인코딩하지 않고 그대로 붙인다")
    void appendsEncodedServiceKeyAsIs() {
        server.expect(requestTo(containsString("serviceKey=TEST%2BKEY")))
                .andRespond(withSuccess(temperatureBody(), MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("serviceKey=TEST%2BKEY")))
                .andRespond(withSuccess(landBody(), MediaType.APPLICATION_JSON));

        assertThat(provider.weather(region, ANNOUNCED.plusDays(4)).available()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("호출이 실패해도 예외를 던지지 않고 available=false를 돌려준다")
    void serverErrorDoesNotPropagate() {
        server.expect(requestTo(containsString("getMidTa"))).andRespond(withServerError());

        assertThat(provider.weather(region, ANNOUNCED.plusDays(4)).available()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("기상청 오류 코드가 오면 available=false를 돌려준다")
    void applicationErrorCodeIsUnavailable() {
        String error = """
                {"response":{"header":{"resultCode":"03","resultMsg":"NO_DATA"}}}
                """;
        server.expect(requestTo(containsString("getMidTa")))
                .andRespond(withSuccess(error, MediaType.APPLICATION_JSON));

        assertThat(provider.weather(region, ANNOUNCED.plusDays(4)).available()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("발표 회차에 따라 예보 시작일이 4일 후 또는 5일 후로 갈린다")
    void resolvesAnnouncementAndFirstForecastDay() {
        MidTermWeatherProvider.Announcement morning =
                MidTermWeatherProvider.latestAnnouncement(LocalDateTime.of(2026, 9, 10, 13, 0));
        assertThat(morning.time()).isEqualTo("0600");
        assertThat(morning.key()).isEqualTo("202609100600");
        assertThat(morning.firstForecastDay()).isEqualTo(4);

        MidTermWeatherProvider.Announcement evening =
                MidTermWeatherProvider.latestAnnouncement(LocalDateTime.of(2026, 9, 10, 23, 0));
        assertThat(evening.time()).isEqualTo("1800");
        assertThat(evening.firstForecastDay()).isEqualTo(5);

        // 06시 전에는 전날 18시 발표가 최신이다. 그 5일 후가 오늘의 4일 후라 빈 날짜가 없다.
        MidTermWeatherProvider.Announcement beforeDawn =
                MidTermWeatherProvider.latestAnnouncement(LocalDateTime.of(2026, 9, 10, 3, 0));
        assertThat(beforeDawn.time()).isEqualTo("1800");
        assertThat(beforeDawn.date()).isEqualTo(LocalDate.of(2026, 9, 9));
        assertThat(beforeDawn.date().plusDays(beforeDawn.firstForecastDay()))
                .isEqualTo(LocalDate.of(2026, 9, 14));

        // 발표 직후에는 아직 자료가 없으므로 이전 회차를 쓴다.
        assertThat(MidTermWeatherProvider.latestAnnouncement(LocalDateTime.of(2026, 9, 10, 6, 10)).time())
                .isEqualTo("1800");
    }

    @Test
    @DisplayName("중기예보 날씨 문자열을 단기예보와 같은 아이콘 어휘로 옮긴다")
    void mapsForecastTextToSharedIconVocabulary() {
        assertThat(MidTermWeatherProvider.icon("맑음")).isEqualTo("SUNNY");
        assertThat(MidTermWeatherProvider.icon("구름많음")).isEqualTo("PARTLY_CLOUDY");
        assertThat(MidTermWeatherProvider.icon("흐림")).isEqualTo("CLOUDY");
        assertThat(MidTermWeatherProvider.icon("구름많고 비")).isEqualTo("RAIN");
        assertThat(MidTermWeatherProvider.icon("흐리고 비")).isEqualTo("RAIN");
        assertThat(MidTermWeatherProvider.icon("구름많고 눈")).isEqualTo("SNOW");
        assertThat(MidTermWeatherProvider.icon("흐리고 눈")).isEqualTo("SNOW");
        // 복합 표현이 "비"·"눈" 단독 판정보다 먼저 걸러져야 한다
        assertThat(MidTermWeatherProvider.icon("구름많고 비/눈")).isEqualTo("SLEET");
        assertThat(MidTermWeatherProvider.icon("흐리고 비/눈")).isEqualTo("SLEET");
        assertThat(MidTermWeatherProvider.icon("구름많고 소나기")).isEqualTo("SHOWER");
        assertThat(MidTermWeatherProvider.icon("흐리고 소나기")).isEqualTo("SHOWER");
        assertThat(MidTermWeatherProvider.icon(null)).isNull();
        assertThat(MidTermWeatherProvider.icon("")).isNull();
    }

    private void expectTemperature() {
        server.expect(requestTo(containsString("getMidTa")))
                .andRespond(withSuccess(temperatureBody(), MediaType.APPLICATION_JSON));
    }

    private void expectLand() {
        server.expect(requestTo(containsString("getMidLandFcst")))
                .andRespond(withSuccess(landBody(), MediaType.APPLICATION_JSON));
    }

    private String temperatureBody() {
        return """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
                "body":{"items":{"item":[{"regId":"11H10501",
                "taMin4":15,"taMax4":26,
                "taMin5":16,"taMax5":27,
                "taMin6":16,"taMax6":28,
                "taMin7":17,"taMax7":28,
                "taMin8":18,"taMax8":26,
                "taMin9":16,"taMax9":26,
                "taMin10":16,"taMax10":25}]}}}}
                """;
    }

    private String landBody() {
        return """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
                "body":{"items":{"item":[{"regId":"11H10000",
                "wf4Am":"맑음","wf4Pm":"맑음",
                "wf5Am":"맑음","wf5Pm":"흐리고 비",
                "wf6Am":"구름많고 눈","wf6Pm":"구름많음",
                "wf7Am":"구름많음","wf7Pm":"구름많음",
                "wf8":"흐림",
                "wf9":"흐리고 비/눈",
                "wf10":"구름많고 소나기"}]}}}}
                """;
    }
}
