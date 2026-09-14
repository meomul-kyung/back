package com.travel.meomulkyung.itinerary.external;
import com.travel.meomulkyung.itinerary.domain.ItineraryItemType; import com.travel.meomulkyung.region.domain.Region; import java.util.*;
public interface TourPlaceProvider {
 List<Place> findPlaces(Region region);
 default List<Place> findAttractions(Region region) { return findPlaces(region).stream().filter(place -> place.type()!=ItineraryItemType.RESTAURANT).toList(); }
 default List<Place> findRestaurants(Region region) { return findPlaces(region).stream().filter(place -> place.type()==ItineraryItemType.RESTAURANT).toList(); }
 record Place(Long contentId,String title,ItineraryItemType type,String imageUrl,String address,Double latitude,Double longitude){}
}
