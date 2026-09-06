package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/**
 * 기상청 단기예보 연동 빈 구성.
 *
 * <p>인증키나 지역 좌표가 비어 있어도 애플리케이션은 정상 기동하며,
 * 이때 {@link KmaWeatherProvider}가 available=false를 돌려준다.
 *
 * <p>ObjectMapper는 컨텍스트에 빈으로 올리지 않는다. 이미 tourApiObjectMapper가 있어
 * 두 개가 되면 ObjectMapper를 주입받는 쪽에서 모호해지기 때문이다.
 */
@Configuration
@EnableConfigurationProperties(KmaWeatherProperties.class)
public class KmaWeatherConfiguration {

    @Bean
    RestClient kmaRestClient(KmaWeatherProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Bean
    WeatherProvider weatherProvider(RestClient kmaRestClient,
                                    KmaWeatherProperties properties,
                                    Clock clock) {
        return new KmaWeatherProvider(kmaRestClient, new ObjectMapper(), properties, clock);
    }
}
