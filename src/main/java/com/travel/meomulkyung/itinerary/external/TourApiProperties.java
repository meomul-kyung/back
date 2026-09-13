package com.travel.meomulkyung.itinerary.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("tour-api")
public class TourApiProperties {
    private String baseUrl = "https://apis.data.go.kr/B551011/KorService2";
    private String serviceKey = "";
    private String mobileOs = "ETC";
    private String mobileApp = "meomul-kyung";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(15);
    private int pageSize = 50;
    private int maxPages = 10;
    private Map<Long, RegionCode> regionCodes = new HashMap<>();

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
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getMaxPages() { return maxPages; }
    public void setMaxPages(int maxPages) { this.maxPages = maxPages; }
    public Map<Long, RegionCode> getRegionCodes() { return regionCodes; }
    public void setRegionCodes(Map<Long, RegionCode> regionCodes) { this.regionCodes = regionCodes; }

    public static class RegionCode {
        private String areaCode;
        private String sigunguCode;

        public RegionCode() { }

        public RegionCode(String areaCode, String sigunguCode) {
            this.areaCode = areaCode;
            this.sigunguCode = sigunguCode;
        }

        public String getAreaCode() { return areaCode; }
        public void setAreaCode(String areaCode) { this.areaCode = areaCode; }
        public String getSigunguCode() { return sigunguCode; }
        public void setSigunguCode(String sigunguCode) { this.sigunguCode = sigunguCode; }
    }
}
