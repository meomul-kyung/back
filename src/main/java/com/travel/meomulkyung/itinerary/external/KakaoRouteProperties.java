package com.travel.meomulkyung.itinerary.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 카카오 경로 조회 설정.
 *
 * <p>자동차 경로는 카카오모빌리티, 대중교통 경로는 카카오맵 REST API를 쓴다.
 * 두 API 모두 카카오디벨로퍼스 앱의 REST API 키를 {@code Authorization: KakaoAK {key}} 헤더로 보낸다.
 * 키가 비어 있어도 애플리케이션은 정상 기동하며, 이때 구간 상태가 UNAVAILABLE로 응답된다.
 */
@ConfigurationProperties("kakao-route")
public class KakaoRouteProperties {

    /** 카카오디벨로퍼스 REST API 키. 프론트엔드에 노출하면 안 된다. */
    private String restApiKey = "";
    private String mobilityBaseUrl = "https://apis-navi.kakaomobility.com";
    private String mapBaseUrl = "https://dapi.kakao.com";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);
    /** 이 거리(m)보다 가까운 구간은 API를 호출하지 않고 NEARBY로 응답한다. */
    private int nearbyDistanceMeters = 300;

    public String getRestApiKey() { return restApiKey; }
    public void setRestApiKey(String restApiKey) { this.restApiKey = restApiKey; }
    public String getMobilityBaseUrl() { return mobilityBaseUrl; }
    public void setMobilityBaseUrl(String mobilityBaseUrl) { this.mobilityBaseUrl = mobilityBaseUrl; }
    public String getMapBaseUrl() { return mapBaseUrl; }
    public void setMapBaseUrl(String mapBaseUrl) { this.mapBaseUrl = mapBaseUrl; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public int getNearbyDistanceMeters() { return nearbyDistanceMeters; }
    public void setNearbyDistanceMeters(int nearbyDistanceMeters) { this.nearbyDistanceMeters = nearbyDistanceMeters; }
}
