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
@EnableConfigurationProperties({KmaWeatherProperties.class, MidTermWeatherProperties.class})
public class KmaWeatherConfiguration {

    @Bean
    RestClient kmaRestClient(KmaWeatherProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Bean
    RestClient kmaMidRestClient(MidTermWeatherProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * 단기예보로 앞날짜를, 중기예보로 뒷날짜를 채운다.
     *
     * <p>두 구현체를 각각 빈으로 올리지 않고 여기서 직접 생성한다.
     * 셋 다 WeatherProvider 타입이라 빈으로 올리면 주입이 모호해지기 때문이다.
     */
    @Bean
    WeatherProvider weatherProvider(RestClient kmaRestClient,
                                    RestClient kmaMidRestClient,
                                    KmaWeatherProperties properties,
                                    MidTermWeatherProperties midProperties,
                                    Clock clock) {
        ObjectMapper objectMapper = new ObjectMapper();
        return new CompositeWeatherProvider(
                new KmaWeatherProvider(kmaRestClient, objectMapper, properties, clock),
                new MidTermWeatherProvider(kmaMidRestClient, objectMapper, midProperties, clock));
    }
}
