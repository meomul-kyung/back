package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 장소 상세(운영시간·휴무일) 조회 빈.
 *
 * <p>기존 TourAPI 설정·RestClient·ObjectMapper를 그대로 재사용한다. 기존 TourApiConfiguration은 수정하지 않는다.
 */
@Configuration
public class PlaceDetailConfiguration {

    @Bean
    PlaceDetailProvider placeDetailProvider(RestClient tourApiRestClient,
                                            @Qualifier("tourApiObjectMapper") ObjectMapper tourApiObjectMapper,
                                            TourApiProperties properties) {
        return new TourApiPlaceDetailProvider(tourApiRestClient, tourApiObjectMapper, properties);
    }
}
