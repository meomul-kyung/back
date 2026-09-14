package com.travel.meomulkyung.itinerary.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ItineraryTimeConfiguration {
    @Bean
    Clock itineraryClock() {

        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
