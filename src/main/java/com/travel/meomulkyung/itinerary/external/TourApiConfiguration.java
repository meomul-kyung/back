package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TourApiProperties.class)
public class TourApiConfiguration {
    @Bean
    RestClient tourApiRestClient(TourApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Bean
    ObjectMapper tourApiObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    TourPlaceProvider tourPlaceProvider(RestClient tourApiRestClient,
                                        @Qualifier("tourApiObjectMapper") ObjectMapper tourApiObjectMapper,
                                        TourApiProperties properties) {
        return new TourApiPlaceProvider(tourApiRestClient, tourApiObjectMapper, properties);
    }

    @Bean
    FestivalProvider festivalProvider(RestClient tourApiRestClient,
                                      @Qualifier("tourApiObjectMapper") ObjectMapper tourApiObjectMapper,
                                      TourApiProperties properties) {
        return new TourApiFestivalProvider(tourApiRestClient, tourApiObjectMapper, properties);
    }
}
