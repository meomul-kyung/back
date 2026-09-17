package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 카카오 경로 조회 빈 구성.
 *
 * <p>ObjectMapper는 빈으로 올리지 않는다. 이미 tourApiObjectMapper가 있어
 * 두 개가 되면 ObjectMapper를 주입받는 쪽에서 모호해지기 때문이다. (KmaWeatherConfiguration과 동일)
 */
@Configuration
@EnableConfigurationProperties(KakaoRouteProperties.class)
public class KakaoRouteConfiguration {

    @Bean
    RestClient kakaoRouteRestClient(KakaoRouteProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Bean
    RouteProvider routeProvider(RestClient kakaoRouteRestClient, KakaoRouteProperties properties) {
        return new KakaoRouteProvider(kakaoRouteRestClient, new ObjectMapper(), properties);
    }
}
