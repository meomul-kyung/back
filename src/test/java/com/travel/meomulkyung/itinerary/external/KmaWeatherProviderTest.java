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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class KmaWeatherProviderTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    // 2026-09-10 13:00 KST → 11시 발표분을 사용해야 한다
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-10T04:00:00Z"), SEOUL);
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 10);

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private KmaWeatherProperties properties;
    private KmaWeatherProvider provider;
    private Region region;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        properties = new KmaWeatherProperties();
        properties.setBaseUrl("https://kma.test/api/typ02/openApi/VilageFcstInfoService_2.0");
        properties.setAuthKey("TEST-AUTH-KEY");
        properties.getCoordinates().put(1L, new KmaWeatherProperties.Coordinate(36.5684, 128.7294));
        provider = new KmaWeatherProvider(builder.build(), new ObjectMapper(), properties, FIXED_CLOCK);
        region = new Region(1L, "안동", null, null, "identity", null);
    }

    @Test
    @DisplayName("단기예보 응답을 하루 단위로 접어 최저·최고 기온과 아이콘을 만든다")
    void foldsHourlyForecastIntoDailyWeather() {
        server.expect(requestTo(containsString("getVilageFcst")))
                .andExpect(queryParam("base_date", "20260910"))
                .andExpect(queryParam("base_time", "1100"))
                .andRespond(withSuccess(body(), MediaType.APPLICATION_JSON));

        WeatherProvider.Weather weather = provider.weather(region, TODAY);

        assertThat(weather.available()).isTrue();
        assertThat(weather.icon()).isEqualTo("SUNNY");
        assertThat(weather.temperature()).isEqualTo(27);
        assertThat(weather.minimumTemperature()).isEqualTo(19);
        assertThat(weather.maximumTemperature()).isEqualTo(29);
        server.verify();
    }

    @Test
    @DisplayName("한 번 호출한 발표 회차는 캐시되어 날짜마다 다시 호출하지 않는다")
    void reusesCachedAnnouncement() {
        server.expect(requestTo(containsString("getVilageFcst")))
                .andRespond(withSuccess(body(), MediaType.APPLICATION_JSON));

        provider.weather(region, TODAY);
        WeatherProvider.Weather second = provider.weather(region, TODAY.plusDays(1));

        assertThat(second.available()).isTrue();
        assertThat(second.icon()).isEqualTo("RAIN");
        // expect()를 한 번만 등록했는데 verify가 통과하면 호출도 한 번뿐이다
        server.verify();
    }

    @Test
    @DisplayName("예보 범위를 벗어난 날짜는 호출 없이 available=false를 돌려준다")
    void outOfRangeDateIsUnavailable() {
        WeatherProvider.Weather weather = provider.weather(region, TODAY.plusDays(4));

        assertThat(weather.available()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("좌표가 없는 지역은 호출하지 않는다")
    void regionWithoutCoordinateIsUnavailable() {
        Region unknown = new Region(99L, "미등록", null, null, "identity", null);

        assertThat(provider.weather(unknown, TODAY).available()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("인증키가 비어 있으면 호출하지 않는다")
    void missingAuthKeyIsUnavailable() {
        properties.setAuthKey("");

        assertThat(provider.weather(region, TODAY).available()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("호출이 실패해도 예외를 던지지 않고 available=false를 돌려준다")
    void serverErrorDoesNotPropagate() {
        server.expect(requestTo(containsString("getVilageFcst"))).andRespond(withServerError());

        assertThat(provider.weather(region, TODAY).available()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("기상청 오류 코드가 오면 available=false를 돌려준다")
    void applicationErrorCodeIsUnavailable() {
        String error = """
                {"response":{"header":{"resultCode":"03","resultMsg":"NO_DATA"}}}
                """;
        server.expect(requestTo(containsString("getVilageFcst")))
                .andRespond(withSuccess(error, MediaType.APPLICATION_JSON));

        assertThat(provider.weather(region, TODAY).available()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("발표 회차는 직전 정시 발표분을 쓰되 제공 지연을 감안한다")
    void resolvesLatestAnnouncement() {
        assertThat(KmaWeatherProvider.latestAnnouncement(LocalDateTime.of(2026, 9, 10, 13, 0)).time())
                .isEqualTo("1100");
        assertThat(KmaWeatherProvider.latestAnnouncement(LocalDateTime.of(2026, 9, 10, 11, 30)).time())
                .isEqualTo("0800");
        assertThat(KmaWeatherProvider.latestAnnouncement(LocalDateTime.of(2026, 9, 10, 23, 50)).time())
                .isEqualTo("2300");

        KmaWeatherProvider.Announcement beforeDawn =
                KmaWeatherProvider.latestAnnouncement(LocalDateTime.of(2026, 9, 10, 1, 0));
        assertThat(beforeDawn.time()).isEqualTo("2300");
        assertThat(beforeDawn.date()).isEqualTo(LocalDate.of(2026, 9, 9));
    }

    /** 오늘은 맑음(27도, 19~29), 내일은 비. */
    private String body() {
        return """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
                "body":{"items":{"item":[
                {"fcstDate":"20260910","fcstTime":"0600","category":"TMN","fcstValue":"19.0"},
                {"fcstDate":"20260910","fcstTime":"1500","category":"TMX","fcstValue":"29.0"},
                {"fcstDate":"20260910","fcstTime":"0900","category":"TMP","fcstValue":"22"},
                {"fcstDate":"20260910","fcstTime":"0900","category":"SKY","fcstValue":"3"},
                {"fcstDate":"20260910","fcstTime":"0900","category":"PTY","fcstValue":"0"},
                {"fcstDate":"20260910","fcstTime":"1200","category":"TMP","fcstValue":"27"},
                {"fcstDate":"20260910","fcstTime":"1200","category":"SKY","fcstValue":"1"},
                {"fcstDate":"20260910","fcstTime":"1200","category":"PTY","fcstValue":"0"},
                {"fcstDate":"20260911","fcstTime":"1200","category":"TMP","fcstValue":"21"},
                {"fcstDate":"20260911","fcstTime":"1200","category":"SKY","fcstValue":"4"},
                {"fcstDate":"20260911","fcstTime":"1200","category":"PTY","fcstValue":"1"}
                ]}}}}
                """;
    }
}
