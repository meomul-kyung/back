package com.travel.meomulkyung.itinerary.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 기상청 중기예보(getMidLandFcst, getMidTa) 연동 설정.
 *
 * <p>단기예보({@link KmaWeatherProperties})와 <b>키 체계가 다르다.</b>
 * 단기예보는 API허브(apihub.kma.go.kr)의 {@code authKey}를 쓰고,
 * 중기예보는 공공데이터포털(apis.data.go.kr)의 {@code serviceKey}를 쓴다.
 * 활용신청도 각각 따로 받으므로 설정을 완전히 분리해 둔다.
 *
 * <p>접두어를 {@code kma-mid}로 둔 이유: {@code kma.mid}로 두면
 * {@code kma} 접두어를 쓰는 {@link KmaWeatherProperties} 바인딩과 키 공간이 겹친다.
 */
@ConfigurationProperties("kma-mid")
public class MidTermWeatherProperties {

    private String baseUrl = "https://apis.data.go.kr/1360000/MidFcstInfoService";
    /** 공공데이터포털 인증키(Encoding 형식). 이미 퍼센트 인코딩된 값이므로 재인코딩하지 않는다. */
    private String serviceKey = "";
    /** 중기육상예보 구역코드. 운영 지역 15곳이 모두 경상북도라 하나로 충분하다. */
    private String landRegionId = "11H10000";
    /** 중기예보가 제공하는 마지막 일자(발표일 기준). 가이드상 10일 후까지다. */
    private int lastForecastDay = 10;
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);
    /** 지역별 중기기온 예보구역코드. 육상예보와 달리 시군 단위다. */
    private Map<Long, String> temperatureRegionIds = new HashMap<>();

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getServiceKey() { return serviceKey; }
    public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
    public String getLandRegionId() { return landRegionId; }
    public void setLandRegionId(String landRegionId) { this.landRegionId = landRegionId; }
    public int getLastForecastDay() { return lastForecastDay; }
    public void setLastForecastDay(int lastForecastDay) { this.lastForecastDay = lastForecastDay; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public Map<Long, String> getTemperatureRegionIds() { return temperatureRegionIds; }
    public void setTemperatureRegionIds(Map<Long, String> temperatureRegionIds) {
        this.temperatureRegionIds = temperatureRegionIds;
    }
}
