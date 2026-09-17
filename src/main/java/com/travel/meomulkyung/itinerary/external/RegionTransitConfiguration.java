package com.travel.meomulkyung.itinerary.external;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 지역 버스 안내 설정 바인딩.
 *
 * <p>이 프로젝트는 {@code @ConfigurationPropertiesScan}을 쓰지 않고
 * 설정 클래스마다 명시적으로 활성화한다. ({@link KakaoRouteConfiguration} 등과 동일)
 */
@Configuration
@EnableConfigurationProperties(RegionTransitProperties.class)
public class RegionTransitConfiguration {
}
