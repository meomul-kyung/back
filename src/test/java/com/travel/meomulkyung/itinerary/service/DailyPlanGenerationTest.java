package com.travel.meomulkyung.itinerary.service;

import com.travel.meomulkyung.itinerary.*;
import com.travel.meomulkyung.itinerary.dto.*;
import com.travel.meomulkyung.itinerary.external.*;
import com.travel.meomulkyung.itinerary.repository.*;
import com.travel.meomulkyung.recommendation.domain.*;
import com.travel.meomulkyung.region.repository.RegionRepository;
import com.travel.meomulkyung.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest @Import({ItineraryService.class,ItineraryTestConfig.class,com.travel.meomulkyung.contribution.ContributionPolicy.class})
class DailyPlanGenerationTest {
 @Autowired ItineraryService service; @Autowired UserRepository users; @Autowired RegionRepository regions; @Autowired ItineraryTestConfig.FakeTourPlaceProvider places; @Autowired ItineraryTestConfig.FakeFestivalProvider festivals; @Autowired Clock clock;
 @BeforeEach void setUp(){regions.save(ItineraryTestFixture.region());places.reset();festivals.setFestivals(List.of());}
 private ItineraryRequests.Create request(int nights){return new ItineraryRequests.Create(1L,LocalDate.now(clock),nights,List.of(PreferenceTag.FOOD),CompanionType.FRIENDS);}
 private Long user(){return users.save(ItineraryTestFixture.user()).getId();}
 @Test void createsTwoDaysWithFiveOrderedVisitsAndPersistentDetails(){var response=service.create(user(),request(1));assertThat(response.days()).hasSize(2);for(var day:response.days()){var visits=day.items().stream().filter(item->item.placeId()!=null||item.festivalId()!=null).toList();assertThat(visits).hasSize(5);assertThat(visits.get(0).type()).isIn("TOURIST_SPOT","EXPERIENCE");assertThat(visits.get(1).type()).isEqualTo("RESTAURANT");assertThat(visits.get(2).type()).isIn("TOURIST_SPOT","EXPERIENCE");assertThat(visits.get(3).type()).isEqualTo("RESTAURANT");assertThat(visits.get(4).type()).isIn("TOURIST_SPOT","EXPERIENCE");assertThat(visits).allSatisfy(item->{assertThat(item.itemId()).isNotNull();assertThat(item.title()).doesNotStartWith("PLACE ");assertThat(item.imageUrl()).isNotNull();assertThat(item.address()).isNotNull();});}assertThat(response.days().getFirst().items().getFirst().type()).isEqualTo("ARRIVAL");assertThat(response.days().getLast().items().getLast().type()).isEqualTo("DEPARTURE");}
 @Test void createsFifteenAttractionsAndTenRestaurantsForFourNightsWithoutDuplicates(){var response=service.create(user(),request(4));var visits=response.days().stream().flatMap(day->day.items().stream()).filter(item->item.placeId()!=null).toList();assertThat(visits).filteredOn(item->item.type().equals("RESTAURANT")).hasSize(10);assertThat(visits).filteredOn(item->!item.type().equals("RESTAURANT")).hasSize(15);assertThat(visits).extracting(ItineraryResponses.Item::placeId).doesNotHaveDuplicates();}
 @Test void distinguishesInsufficientAttractionsAndRestaurants(){List<TourPlaceProvider.Place> restaurants=java.util.stream.LongStream.rangeClosed(101,104).mapToObj(id->new TourPlaceProvider.Place(id,"R",com.travel.meomulkyung.itinerary.domain.ItineraryItemType.RESTAURANT,null,null,null,null)).toList();places.setCandidates(restaurants);assertThatThrownBy(()->service.create(user(),request(1))).hasFieldOrPropertyWithValue("code","NOT_ENOUGH_ATTRACTIONS");List<TourPlaceProvider.Place> attractions=java.util.stream.LongStream.rangeClosed(1,6).mapToObj(id->new TourPlaceProvider.Place(id,"A",com.travel.meomulkyung.itinerary.domain.ItineraryItemType.TOURIST_SPOT,null,null,null,null)).toList();places.setCandidates(attractions);assertThatThrownBy(()->service.create(user(),request(1))).hasFieldOrPropertyWithValue("code","NOT_ENOUGH_RESTAURANTS");}
 @Test void festivalReplacesOneAttractionWithoutAddingSixthVisit(){LocalDate start=LocalDate.now(clock);festivals.setFestivals(List.of(new FestivalProvider.Festival(999L,"Festival",start,start,null,null),new FestivalProvider.Festival(998L,"Second",start,start,null,null)));var response=service.create(user(),request(1));var first=response.days().getFirst().items().stream().filter(item->item.placeId()!=null||item.festivalId()!=null).toList();assertThat(first).hasSize(5);assertThat(first).filteredOn(item->item.type().equals("FESTIVAL")).hasSize(1);}
 @Test void reusesPlacesWithWarningWhenRegionDataIsShort(){List<TourPlaceProvider.Place> pool=new ArrayList<>(java.util.stream.LongStream.rangeClosed(1,6).mapToObj(id->new TourPlaceProvider.Place(id,"A"+id,com.travel.meomulkyung.itinerary.domain.ItineraryItemType.TOURIST_SPOT,null,null,null,null)).toList());pool.addAll(java.util.stream.LongStream.rangeClosed(101,103).mapToObj(id->new TourPlaceProvider.Place(id,"R"+id,com.travel.meomulkyung.itinerary.domain.ItineraryItemType.RESTAURANT,null,null,null,null)).toList());places.setCandidates(pool);var response=service.create(user(),request(1));var restaurants=response.days().stream().flatMap(day->day.items().stream()).filter(item->"RESTAURANT".equals(item.type())).toList();assertThat(restaurants).hasSize(4);assertThat(restaurants.stream().map(ItineraryResponses.Item::placeId).distinct()).hasSize(3);assertThat(response.warnings()).extracting(ItineraryResponses.Warning::code).contains("PLACE_REUSED");}
 @Test void regenerateProducesDifferentPlaces(){Long u=user();var first=service.create(u,request(1));List<Long> before=placeIds(first);var second=service.regenerate(u,first.itineraryId(),new ItineraryRequests.Replace(false));List<Long> after=placeIds(second);assertThat(second.generationVersion()).isEqualTo(2);assertThat(after).hasSameSizeAs(before);assertThat(after).doesNotContainAnyElementsOf(before);}
 private List<Long> placeIds(ItineraryResponses.Itinerary r){return r.days().stream().flatMap(d->d.items().stream()).map(ItineraryResponses.Item::placeId).filter(Objects::nonNull).toList();}
}
