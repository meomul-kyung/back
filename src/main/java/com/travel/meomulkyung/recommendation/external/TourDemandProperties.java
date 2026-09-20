package com.travel.meomulkyung.recommendation.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 「지역별 관광 수요 강도」 연동 설정.
 *
 * <p>같은 공공데이터포털이지만 TourAPI 국문 관광정보 서비스와 <b>인증키가 다르다</b>.
 * 활용신청이 서비스별로 이뤄지고 키도 그 서비스에만 등록되기 때문에,
 * {@code tour-api.service-key}를 그대로 쓰면 {@code SERVICE_KEY_IS_NOT_REGISTERED_ERROR}가 난다.
 *
 * <p>지역 코드 체계도 다르다. 관광정보 서비스는 관광공사 자체 코드(경북 35, 영덕 12)를 쓰지만
 * 이쪽은 행정표준코드(경북 47, 영덕 47770)를 쓴다. 그래서 {@code tour-api.region-codes}를
 * 재사용하지 못하고 {@code signgu-codes}를 따로 둔다.
 */
@ConfigurationProperties("tour-demand")
public class TourDemandProperties {

    private String baseUrl = "https://apis.data.go.kr/B551011/AreaTarDemDsService";
    private String serviceKey = "";
    private String mobileOs = "ETC";
    private String mobileApp = "meomul-kyung";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(10);
    /**
     * 조회 기준 연월(YYYYMM). 데이터가 월 1회(매월 16일) 갱신되므로 실시간성이 없다.
     * 자동 탐색 대신 상수로 두고, 갱신할 때 이 값만 바꾼다.
     */
    private String baseYm = "202608";
    /** 운영 지역이 모두 경상북도라 시도 코드는 하나다. */
    private String areaCd = "47";
    /** {@code region_id} → 행정표준 시군구 코드. */
    private Map<Long, String> signguCodes = new HashMap<>();

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getServiceKey() { return serviceKey; }
    public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
    public String getMobileOs() { return mobileOs; }
    public void setMobileOs(String mobileOs) { this.mobileOs = mobileOs; }
    public String getMobileApp() { return mobileApp; }
    public void setMobileApp(String mobileApp) { this.mobileApp = mobileApp; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public String getBaseYm() { return baseYm; }
    public void setBaseYm(String baseYm) { this.baseYm = baseYm; }
    public String getAreaCd() { return areaCd; }
    public void setAreaCd(String areaCd) { this.areaCd = areaCd; }
    public Map<Long, String> getSignguCodes() { return signguCodes; }
    public void setSignguCodes(Map<Long, String> signguCodes) { this.signguCodes = signguCodes; }
}
