package com.travel.meomulkyung.itinerary.external;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UnavailableProviders {
    @Bean
    WeatherProvider weatherProvider() {
        return (region, date) -> new WeatherProvider.Weather(false, null, null, null, null);
    }
}
