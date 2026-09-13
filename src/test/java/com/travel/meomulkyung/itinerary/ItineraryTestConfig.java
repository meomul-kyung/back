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
 @Bean @Primary FakeTourPlaceProvider fakeTourPlaceProvider(){return new FakeTourPlaceProvider();}
 @Bean @Primary FakeFestivalProvider fakeFestivalProvider(){return new FakeFestivalProvider();}
 @Bean @Primary WeatherProvider fakeWeatherProvider(){return (r,d)->new WeatherProvider.Weather(true,"SUNNY",18,12,22);}
 public static class FakeFestivalProvider implements FestivalProvider {
  private List<Festival> festivals=List.of(); private RuntimeException failure;
  public void setFestivals(List<Festival> festivals){this.festivals=festivals;failure=null;}
  public void fail(){failure=new IllegalStateException("provider unavailable");}
  @Override public List<Festival> findFestivals(Region region,LocalDate start,LocalDate end){if(failure!=null)throw failure;return festivals;}
 }
 public static class FakeTourPlaceProvider implements TourPlaceProvider {
  private RuntimeException failure; private List<Place> candidates;
  public void fail(RuntimeException exception){failure=exception;}
  public void reset(){failure=null;candidates=null;}
  public void setCandidates(List<Place> candidates){this.candidates=candidates;}
  @Override public List<Place> findPlaces(Region region){if(failure!=null)throw failure;if(candidates!=null)return candidates;List<Place> result=new java.util.ArrayList<>();for(long id=1;id<=30;id++)result.add(new Place(id,"Attraction "+id,id%4==0?ItineraryItemType.EXPERIENCE:ItineraryItemType.TOURIST_SPOT,"https://image.example/"+id,"Address "+id,null,null));for(long id=101;id<=120;id++)result.add(new Place(id,"Restaurant "+id,ItineraryItemType.RESTAURANT,"https://image.example/"+id,"Address "+id,null,null));return result;}
 }
}
