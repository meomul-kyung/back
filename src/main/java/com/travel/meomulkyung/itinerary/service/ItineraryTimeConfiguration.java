package com.travel.meomulkyung.itinerary.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ItineraryTimeConfiguration {
    @Bean
    Clock itineraryClock() {
        return Clock.systemDefaultZone();
    }
}
