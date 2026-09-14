package com.travel.meomulkyung.itinerary.external;

import com.travel.meomulkyung.region.domain.Region;

import java.time.LocalDate;
import java.util.List;

/**
 * 여러 날씨 제공자를 앞에서부터 시도해 처음으로 값이 나온 결과를 쓴다.
 *
 * <p>단기예보(오늘 ~ 3일 후)와 중기예보(4일 후 ~ 10일 후)를 이어 붙이기 위한 것이다.
 * 날짜 경계를 여기서 다시 계산하지 않고 <b>각 제공자가 스스로 판단해 available=false를
 * 돌려주는 것</b>에 맡긴다. 경계 조건이 두 곳에 중복되면 한쪽만 고쳤을 때 빈 날짜가 생긴다.
 *
 * <p>순서가 의미를 가진다. 단기예보가 중기예보보다 정확하므로 겹치는 날짜에서는
 * 단기예보가 이긴다.
 */
public class CompositeWeatherProvider implements WeatherProvider {

    private static final Weather UNAVAILABLE = new Weather(false, null, null, null, null);

    private final List<WeatherProvider> providers;

    public CompositeWeatherProvider(WeatherProvider... providers) {
        this.providers = List.of(providers);
    }

    @Override
    public Weather weather(Region region, LocalDate date) {
        for (WeatherProvider provider : providers) {
            Weather weather = provider.weather(region, date);
            if (weather.available()) {
                return weather;
            }
        }
        return UNAVAILABLE;
    }
}
