package com.travel.meomulkyung.itinerary.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 기상청 API허브(apihub.kma.go.kr) 단기예보 연동 설정.
 *
 * <p>TourAPI(공공데이터포털, {@code serviceKey})와는 별개의 체계다.
 * API허브는 계정 단위 {@code authKey} 하나를 쓰며 호스트와 인증 파라미터 이름이 다르므로,
 * TourAPI 설정({@link TourApiProperties})과 키·호스트를 완전히 분리해 둔다.
 */
@ConfigurationProperties("kma")
public class KmaWeatherProperties {

    private String baseUrl = "https://apihub.kma.go.kr/api/typ02/openApi/VilageFcstInfoService_2.0";
    /** API허브 인증키. TourAPI의 serviceKey와 다른 값이다. */
    private String authKey = "";
    /** 단기예보 제공 범위(일). 오늘(0)부터 이 값까지 예보를 제공한다. */
    private int forecastDays = 3;
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);
    private int pageSize = 1000;
    /** 지역별 대표 좌표. 격자(nx, ny)는 KmaGridConverter가 계산한다. */
    private Map<Long, Coordinate> coordinates = new HashMap<>();

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getAuthKey() { return authKey; }
    public void setAuthKey(String authKey) { this.authKey = authKey; }
    public int getForecastDays() { return forecastDays; }
    public void setForecastDays(int forecastDays) { this.forecastDays = forecastDays; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public Map<Long, Coordinate> getCoordinates() { return coordinates; }
    public void setCoordinates(Map<Long, Coordinate> coordinates) { this.coordinates = coordinates; }

    public static class Coordinate {
        private Double latitude;
        private Double longitude;

        public Coordinate() { }

        public Coordinate(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }
}
