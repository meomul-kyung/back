package com.travel.meomulkyung.recommendation.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TourDemandProperties.class)
public class TourDemandConfiguration {

    @Bean
    RestClient tourDemandRestClient(TourDemandProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    // ObjectMapper는 빈으로 올리지 않는다. 이 연동에서만 쓰는 도구인데 빈으로 만들면
    // 컨텍스트에 타입이 같은 후보가 늘어 @Autowired ObjectMapper 가 모호해진다.
    @Bean
    TourDemandProvider tourDemandProvider(RestClient tourDemandRestClient, TourDemandProperties properties) {
        return new TourApiDemandProvider(tourDemandRestClient, new ObjectMapper(), properties);
    }
}
