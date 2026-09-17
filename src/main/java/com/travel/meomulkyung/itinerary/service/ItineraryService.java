package com.travel.meomulkyung.itinerary.service;

import com.travel.meomulkyung.contribution.*;
import com.travel.meomulkyung.itinerary.domain.*;
import com.travel.meomulkyung.itinerary.dto.*;
import com.travel.meomulkyung.itinerary.external.*;
import com.travel.meomulkyung.itinerary.repository.*;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.repository.RegionRepository;
import com.travel.meomulkyung.user.domain.User;
import com.travel.meomulkyung.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.*;

@Service @RequiredArgsConstructor
public class ItineraryService {
 private final ItineraryRepository itineraries; private final ItineraryItemRepository items; private final RegionRepository regions; private final UserRepository users; private final CompletedTripRepository completed; private final UserStampRepository stamps; private final TourPlaceProvider places; private final FestivalProvider festivals; private final WeatherProvider weather; private final Clock clock; private final ContributionPolicy contributionPolicy;
 @Transactional public ItineraryResponses.Itinerary create(Long uid,ItineraryRequests.Create r){if(r.duplicate())throw error(HttpStatus.BAD_REQUEST,"INVALID_PREFERENCE_TAGS","취향 태그는 중복될 수 없습니다.");Region region=regions.findById(r.regionId()).orElseThrow(()->error(HttpStatus.NOT_FOUND,"REGION_NOT_FOUND","지역을 찾을 수 없습니다."));User user=users.findById(uid).orElseThrow(()->error(HttpStatus.UNAUTHORIZED,"ACCESS_TOKEN_INVALID","인증 사용자를 찾을 수 없습니다."));Itinerary itinerary=itineraries.save(new Itinerary(user,region,r.startDate(),r.nights(),r.preferenceTags(),r.companionType()));List<ItineraryResponses.Warning>warnings=generate(itinerary);itineraries.saveAndFlush(itinerary);return response(itinerary,warnings);}
 @Transactional public ItineraryResponses.Itinerary get(Long uid,Long id){return response(owner(uid,id),List.of());}
 @Transactional public ItineraryResponses.Replacement replace(Long uid,Long iid,Long itemId,ItineraryRequests.Replace request){Itinerary itinerary=owner(uid,iid);if(itinerary.getStatus()==ItineraryStatus.COMPLETED)throw error(HttpStatus.CONFLICT,"ITINERARY_ALREADY_COMPLETED","완료된 일정은 변경할 수 없습니다.");ItineraryItem old=itinerary.getItems().stream().filter(item->item.getId().equals(itemId)).findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"ITINERARY_ITEM_NOT_FOUND","일정 항목을 찾을 수 없습니다."));if(!old.isReplaceable())throw error(HttpStatus.CONFLICT,"ITEM_NOT_REPLACEABLE","교체할 수 없는 항목입니다.");Set<Long>excluded=itinerary.getItems().stream().map(ItineraryItem::getContentId).filter(Objects::nonNull).collect(Collectors.toSet());List<TourPlaceProvider.Place>source=old.getItemType()==ItineraryItemType.RESTAURANT?places.findRestaurants(itinerary.getRegion()):places.findAttractions(itinerary.getRegion());List<TourPlaceProvider.Place>candidates=unique(source).stream().filter(p->p.type()==old.getItemType()).filter(p->!excluded.contains(p.contentId())).toList();if(candidates.isEmpty())throw error(HttpStatus.UNPROCESSABLE_ENTITY,"NOT_ENOUGH_PLACES","대체 후보가 부족합니다.");TourPlaceProvider.Place place=candidates.get(RoutePlanner.nearestIndex(candidates,ItineraryService::point,neighbourAnchor(itinerary,old)));old.replace(place.contentId(),place.title(),place.imageUrl(),place.address(),place.latitude(),place.longitude(),place.contentTypeId());itinerary.incrementGenerationVersion();return new ItineraryResponses.Replacement(itinerary.getId(),old.getId(),item(old),itinerary.getGenerationVersion());}
 @Transactional public ItineraryResponses.Itinerary regenerate(Long uid,Long id,ItineraryRequests.Replace r){Itinerary itinerary=owner(uid,id);if(itinerary.getStatus()==ItineraryStatus.COMPLETED)throw error(HttpStatus.CONFLICT,"ITINERARY_ALREADY_COMPLETED","완료된 일정은 재생성할 수 없습니다.");itinerary.incrementGenerationVersion();List<ItineraryResponses.Warning>warnings=generate(itinerary);itineraries.flush();return response(itinerary,warnings);}
 @Transactional public ItineraryResponses.Bookmark bookmark(Long uid,Long id){Itinerary i=owner(uid,id);i.bookmark();return new ItineraryResponses.Bookmark(i.getId(),true,i.getBookmarkedAt());}
 @Transactional public void unbookmark(Long uid,Long id){owner(uid,id).unbookmark();}
 @Transactional public ItineraryResponses.Completion complete(Long uid,Long id,ItineraryRequests.Completion r){Itinerary i=owner(uid,id);if(completed.existsByItineraryId(id)||i.getStatus()==ItineraryStatus.COMPLETED)throw error(HttpStatus.CONFLICT,"ITINERARY_ALREADY_COMPLETED","이미 완료된 일정입니다.");i.complete();CompletedTrip trip=completed.save(new CompletedTrip(i,r.stayHours(),r.partySize(),r.totalSpent()));ContributionResult c=contributionPolicy.calculate(r.stayHours(),r.partySize());trip.applyContribution(c.estimatedSpending(),c.populationContributionDays(),c.policyVersion());Optional<UserStamp>existing=stamps.findByUserIdAndRegionId(uid,i.getRegion().getId());UserStamp stamp=existing.orElseGet(()->stamps.save(new UserStamp(i.getUser(),i.getRegion())));if(existing.isPresent())stamp.visit();return new ItineraryResponses.Completion(trip.getId(),id,region(i.getRegion()),r.stayHours(),r.partySize(),r.totalSpent(),new ItineraryResponses.Stamp(true,existing.isEmpty(),i.getRegion().getId(),i.getRegion().getName(),stamp.getVisitCount()),new ItineraryResponses.Contribution(c.populationContributionDays(),"CALCULATED",c.policyVersion()),trip.getCompletedAt());}
 public ItineraryResponses.WeatherDays weather(Long uid,Long id){Itinerary i=owner(uid,id);LocalDate now=LocalDate.now(clock);return new ItineraryResponses.WeatherDays(days(i).map(d->{if(d.isAfter(now.plusDays(10)))return new ItineraryResponses.WeatherDay(d,false,null,null);WeatherProvider.Weather w=weather.weather(i.getRegion(),d);return new ItineraryResponses.WeatherDay(d,w.available(),w.icon(),w.temperature());}).toList());}
 private List<ItineraryResponses.Warning> generate(Itinerary i){int days=i.getNights()+1;List<FestivalProvider.Festival>matched;List<ItineraryResponses.Warning>warnings=List.of();try{matched=festivals.findFestivals(i.getRegion(),i.getStartDate(),i.getEndDate()).stream().filter(f->overlaps(i,f)).toList();}catch(RuntimeException e){matched=List.of();warnings=List.of(new ItineraryResponses.Warning("FESTIVAL_DATA_UNAVAILABLE","축제 정보를 일정에 반영하지 못했습니다."));}Map<Integer,FestivalProvider.Festival>festivalByDay=new LinkedHashMap<>();Set<Long>festivalIds=new HashSet<>();for(FestivalProvider.Festival f:matched){int day=festivalDay(i,f);if(!festivalByDay.containsKey(day)&&festivalIds.add(f.contentId()))festivalByDay.put(day,f);}List<TourPlaceProvider.Place>attractions=unique(places.findAttractions(i.getRegion())).stream().filter(p->!festivalIds.contains(p.contentId())).toList();int requiredAttractions=days*3-festivalByDay.size(),requiredRestaurants=days*2;if(attractions.isEmpty())throw error(HttpStatus.UNPROCESSABLE_ENTITY,"NOT_ENOUGH_ATTRACTIONS","관광지 후보가 부족합니다.");Set<Long>attractionIds=attractions.stream().map(TourPlaceProvider.Place::contentId).collect(Collectors.toSet());List<TourPlaceProvider.Place>restaurantPool=unique(places.findRestaurants(i.getRegion())).stream().filter(p->!festivalIds.contains(p.contentId())&&!attractionIds.contains(p.contentId())).toList();if(restaurantPool.isEmpty())throw error(HttpStatus.UNPROCESSABLE_ENTITY,"NOT_ENOUGH_RESTAURANTS","음식점 후보가 부족합니다.");boolean reused=attractions.size()<requiredAttractions||restaurantPool.size()<requiredRestaurants;int rotation=i.getGenerationVersion();attractions=fill(attractions,requiredAttractions,rotation);List<TourPlaceProvider.Place>restaurants=fill(restaurantPool,requiredRestaurants,rotation);if(reused){List<ItineraryResponses.Warning>merged=new ArrayList<>(warnings);merged.add(new ItineraryResponses.Warning("PLACE_REUSED","지역의 장소 데이터가 부족해 일부 장소가 반복됩니다."));warnings=List.copyOf(merged);}i.replaceItems(arrange(i,days,festivalByDay,attractions,restaurants,!reused));return warnings;}

 /**
  * 뽑힌 장소를 날짜와 칸에 배치한다.
  *
  * <p><b>무엇을 뽑을지는 {@link #fill}이 이미 정했고, 여기서는 "어느 날 · 어떤 순서"만 정한다.</b>
  * 뽑기 규칙을 건드리지 않아 재생성 시 장소가 전부 바뀌는 기존 보장이 그대로 유지된다.
  *
  * <p>{@code routeAware}가 false면 뽑힌 순서를 그대로 쓴다. 기존 동작과 완전히 같다.
  * 장소가 부족해 같은 곳이 반복되는 지역({@code PLACE_REUSED})에서 false가 되는데,
  * 좌표로 묶으면 같은 장소가 같은 날에 몰려 오히려 지금보다 나빠지기 때문이다.
  * 좌표가 없는 장소만 섞여 있는 경우는 {@link RoutePlanner}가 뒤로 밀어 처리한다.
  *
  * <p>하루 칸 구성은 바꾸지 않는다. [도착] → 축제|관광 → 식당 → 관광 → 식당 → 관광 → [휴식] → [출발].
  * 축제가 있는 날은 첫 칸이 축제라 관광이 2곳이고, 축제 항목에는 좌표가 없어 정렬 대상에서 빠진다.
  */
 private List<ItineraryItem> arrange(Itinerary i,int days,Map<Integer,FestivalProvider.Festival>festivalByDay,
                                     List<TourPlaceProvider.Place>attractions,List<TourPlaceProvider.Place>restaurants,
                                     boolean routeAware){
  List<TourPlaceProvider.Place>ordered=routeAware
          ?RoutePlanner.orderByProximity(attractions,ItineraryService::point)
          :attractions;
  List<TourPlaceProvider.Place>restaurantPool=new ArrayList<>(restaurants);
  List<ItineraryItem>next=new ArrayList<>();
  int ai=0;
  for(int day=1;day<=days;day++){
   FestivalProvider.Festival festival=festivalByDay.get(day);
   int shift=festival==null?0:1;
   List<TourPlaceProvider.Place>dayAttractions=List.copyOf(ordered.subList(ai,ai+3-shift));
   ai+=3-shift;
   int seq=1;
   if(day==1)next.add(new ItineraryItem(i,day,seq++,ItineraryItemType.ARRIVAL,null,"ARRIVAL"));
   if(festival!=null)next.add(festivalItem(i,day,seq++,festival));
   else next.add(placeItem(i,day,seq++,dayAttractions.get(0)));
   for(int slot=0;slot<2;slot++){
    RoutePlanner.Point between=routeAware?mealAnchor(dayAttractions,slot,shift):null;
    next.add(placeItem(i,day,seq++,restaurantPool.remove(RoutePlanner.nearestIndex(restaurantPool,ItineraryService::point,between))));
    next.add(placeItem(i,day,seq++,dayAttractions.get(slot+1-shift)));
   }
   if(day<=i.getNights())next.add(new ItineraryItem(i,day,seq++,ItineraryItemType.REST,null,"REST"));
   if(day==days)next.add(new ItineraryItem(i,day,seq,ItineraryItemType.DEPARTURE,null,"DEPARTURE"));
  }
  return next;
 }

 /** 식사 칸 앞뒤 관광지의 중간 지점. 축제가 앞에 오는 첫 칸처럼 한쪽이 비면 나머지 한 곳을 기준으로 삼는다. */
 private RoutePlanner.Point mealAnchor(List<TourPlaceProvider.Place>dayAttractions,int slot,int shift){
  RoutePlanner.Point before=slot-shift>=0?point(dayAttractions.get(slot-shift)):null;
  return RoutePlanner.midpoint(before,point(dayAttractions.get(slot+1-shift)));
 }

 /** 교체할 항목의 앞뒤(같은 날, 좌표가 있는 가장 가까운 항목)의 중간 지점. 기준이 없으면 null이다. */
 private RoutePlanner.Point neighbourAnchor(Itinerary itinerary,ItineraryItem target){
  List<ItineraryItem>sameDay=itinerary.getItems().stream()
          .filter(item->item.getDayNumber()==target.getDayNumber())
          .sorted(Comparator.comparingInt(ItineraryItem::getSequence)).toList();
  int index=sameDay.indexOf(target);
  RoutePlanner.Point before=null,after=null;
  for(int k=index-1;k>=0&&before==null;k--)before=point(sameDay.get(k));
  for(int k=index+1;k<sameDay.size()&&after==null;k++)after=point(sameDay.get(k));
  return RoutePlanner.midpoint(before,after);
 }

 private static RoutePlanner.Point point(TourPlaceProvider.Place p){return RoutePlanner.point(p.latitude(),p.longitude());}
 private static RoutePlanner.Point point(ItineraryItem i){return RoutePlanner.point(i.getLatitude(),i.getLongitude());}
 private List<TourPlaceProvider.Place> fill(List<TourPlaceProvider.Place>pool,int required,int rotation){if(required<=0)return List.of();long base=(long)rotation*required;List<TourPlaceProvider.Place>filled=new ArrayList<>(required);for(int k=0;k<required;k++)filled.add(pool.get((int)Math.floorMod(base+k,pool.size())));return List.copyOf(filled);}
 private List<TourPlaceProvider.Place> unique(List<TourPlaceProvider.Place>s){Map<Long,TourPlaceProvider.Place>values=new LinkedHashMap<>();s.forEach(p->values.putIfAbsent(p.contentId(),p));return List.copyOf(values.values());}
 private ItineraryItem placeItem(Itinerary i,int day,int seq,TourPlaceProvider.Place p){return new ItineraryItem(i,day,seq,p.type(),p.contentId(),p.title(),p.imageUrl(),p.address(),p.latitude(),p.longitude(),p.contentTypeId());}
 private ItineraryItem festivalItem(Itinerary i,int day,int seq,FestivalProvider.Festival f){return new ItineraryItem(i,day,seq,f.contentId(),f.title(),f.imageUrl(),f.address(),f.startDate(),f.endDate());}
 private boolean overlaps(Itinerary i,FestivalProvider.Festival f){return f.startDate()!=null&&f.endDate()!=null&&!f.startDate().isAfter(i.getEndDate())&&!f.endDate().isBefore(i.getStartDate());}
 private int festivalDay(Itinerary i,FestivalProvider.Festival f){LocalDate d=f.startDate().isBefore(i.getStartDate())?i.getStartDate():f.startDate();return(int)ChronoUnit.DAYS.between(i.getStartDate(),d)+1;}
 private Itinerary owner(Long uid,Long id){Itinerary i=itineraries.findDetailById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"ITINERARY_NOT_FOUND","일정을 찾을 수 없습니다."));if(!i.getUser().getId().equals(uid))throw error(HttpStatus.FORBIDDEN,"ITINERARY_ACCESS_DENIED","일정 접근 권한이 없습니다.");return i;}
 private ItineraryResponses.Itinerary response(Itinerary i,List<ItineraryResponses.Warning>warnings){return new ItineraryResponses.Itinerary(i.getId(),i.getStatus().name(),i.isBookmarked(),region(i.getRegion()),i.getTitle(),i.getStartDate(),i.getEndDate(),i.getNights(),i.getGenerationVersion(),days(i).map(d->new ItineraryResponses.Day((int)ChronoUnit.DAYS.between(i.getStartDate(),d)+1,d,weather(i.getRegion(),d),i.getItems().stream().filter(x->x.getDayNumber()==(int)ChronoUnit.DAYS.between(i.getStartDate(),d)+1).map(this::item).toList())).toList(),warnings);}
 private ItineraryResponses.Weather weather(Region r,LocalDate d){WeatherProvider.Weather w=weather.weather(r,d);return new ItineraryResponses.Weather(w.available(),w.icon(),w.minimumTemperature(),w.maximumTemperature());}
 private Stream<LocalDate> days(Itinerary i){return i.getStartDate().datesUntil(i.getEndDate().plusDays(1));}
 private ItineraryResponses.Region region(Region r){return new ItineraryResponses.Region(r.getId(),r.getName());}
 private ItineraryResponses.Item item(ItineraryItem i){String title=i.getCustomTitle()!=null?i.getCustomTitle():"장소 정보 없음";return new ItineraryResponses.Item(i.getId(),i.getSequence(),i.getItemType().name(),title,i.getItemType()==ItineraryItemType.FESTIVAL?null:i.getContentId(),i.getItemType()==ItineraryItemType.FESTIVAL?i.getContentId():null,i.getImageUrl(),i.getAddress(),i.getLatitude(),i.getLongitude(),i.getEventStartDate(),i.getEventEndDate(),null,i.isReplaceable());}
 private ItineraryException error(HttpStatus status,String code,String message){return new ItineraryException(status,code,message);}
}
