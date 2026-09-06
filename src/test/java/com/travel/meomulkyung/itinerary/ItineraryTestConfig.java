package com.travel.meomulkyung.itinerary;

import com.travel.meomulkyung.itinerary.domain.ItineraryItemType;
import com.travel.meomulkyung.itinerary.external.FestivalProvider;
import com.travel.meomulkyung.itinerary.external.TourPlaceProvider;
import com.travel.meomulkyung.itinerary.external.WeatherProvider;
import com.travel.meomulkyung.region.domain.Region;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@TestConfiguration
public class ItineraryTestConfig {
 public static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-30T00:00:00Z"), ZoneOffset.UTC);
 @Bean @Primary Clock clock(){return FIXED_CLOCK;}
 @Bean @Primary TourPlaceProvider fakeTourPlaceProvider(){return r->List.of(new TourPlaceProvider.Place(1L,"A",ItineraryItemType.TOURIST_SPOT,null,null,null,null),new TourPlaceProvider.Place(2L,"B",ItineraryItemType.TOURIST_SPOT,null,null,null,null),new TourPlaceProvider.Place(3L,"C",ItineraryItemType.RESTAURANT,null,null,null,null),new TourPlaceProvider.Place(4L,"D",ItineraryItemType.EXPERIENCE,null,null,null,null),new TourPlaceProvider.Place(5L,"E",ItineraryItemType.TOURIST_SPOT,null,null,null,null),new TourPlaceProvider.Place(6L,"F",ItineraryItemType.TOURIST_SPOT,null,null,null,null),new TourPlaceProvider.Place(7L,"G",ItineraryItemType.TOURIST_SPOT,null,null,null,null),new TourPlaceProvider.Place(8L,"H",ItineraryItemType.TOURIST_SPOT,null,null,null,null),new TourPlaceProvider.Place(9L,"I",ItineraryItemType.TOURIST_SPOT,null,null,null,null));}
 @Bean @Primary FakeFestivalProvider fakeFestivalProvider(){return new FakeFestivalProvider();}
 @Bean @Primary WeatherProvider fakeWeatherProvider(){return (r,d)->new WeatherProvider.Weather(true,"SUNNY",18,12,22);}
 public static class FakeFestivalProvider implements FestivalProvider {
  private List<Festival> festivals=List.of(); private RuntimeException failure;
  public void setFestivals(List<Festival> festivals){this.festivals=festivals;failure=null;}
  public void fail(){failure=new IllegalStateException("provider unavailable");}
  @Override public List<Festival> findFestivals(Region region,LocalDate start,LocalDate end){if(failure!=null)throw failure;return festivals;}
 }
}
