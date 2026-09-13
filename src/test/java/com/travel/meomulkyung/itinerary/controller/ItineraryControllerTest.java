package com.travel.meomulkyung.itinerary.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.travel.meomulkyung.global.security.SecurityConfig;
import com.travel.meomulkyung.global.security.jwt.JwtAuthenticationFilter;
import com.travel.meomulkyung.global.security.jwt.JwtTokenProvider;
import com.travel.meomulkyung.global.security.oauth.CustomOAuth2UserService;
import com.travel.meomulkyung.global.security.oauth.OAuth2FailureHandler;
import com.travel.meomulkyung.global.security.oauth.OAuth2SuccessHandler;
import com.travel.meomulkyung.itinerary.ItineraryTestConfig;
import com.travel.meomulkyung.itinerary.ItineraryTestFixture;
import com.travel.meomulkyung.itinerary.domain.ItineraryItem;
import com.travel.meomulkyung.itinerary.domain.ItineraryItemType;
import com.travel.meomulkyung.itinerary.external.TourApiProviderException;
import com.travel.meomulkyung.itinerary.repository.ItineraryRepository;
import com.travel.meomulkyung.region.repository.RegionRepository;
import com.travel.meomulkyung.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
 "spring.datasource.url=jdbc:h2:mem:itinerary-controller;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
 "spring.datasource.driver-class-name=org.h2.Driver",
 "spring.datasource.username=sa",
 "spring.datasource.password=",
 "spring.jpa.hibernate.ddl-auto=create-drop",
 "spring.jpa.show-sql=false",
 "logging.level.org.springframework=warn",
 "logging.level.org.hibernate=warn"
})
@AutoConfigureMockMvc
@Import({ItineraryTestConfig.class, SecurityConfig.class})
class ItineraryControllerTest {
 private static final LocalDate START_DATE = LocalDate.now(ItineraryTestConfig.FIXED_CLOCK).plusDays(11);
 @Autowired MockMvc mockMvc;
 @Autowired UserRepository users;
 @Autowired RegionRepository regions;
 @Autowired ItineraryRepository itineraries;
 @Autowired ItineraryTestConfig.FakeFestivalProvider festivals;
 @Autowired ItineraryTestConfig.FakeTourPlaceProvider places;
 @MockitoBean CustomOAuth2UserService customOAuth2UserService;
 @MockitoBean OAuth2SuccessHandler oAuth2SuccessHandler;
 @MockitoBean OAuth2FailureHandler oAuth2FailureHandler;
 @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
 @MockitoBean JwtTokenProvider jwtTokenProvider;

 @BeforeEach void setUp() throws Exception {
  if (!regions.existsById(1L)) regions.save(ItineraryTestFixture.region());
  festivals.setFestivals(List.of());
  places.reset();
  doAnswer(invocation -> {
   var request = (jakarta.servlet.http.HttpServletRequest) invocation.getArgument(0);
   SecurityContextHolder.clearContext();
   String authorization = request.getHeader("Authorization");
   if (authorization != null && authorization.startsWith("Bearer test-user-")) {
    Long userId = Long.valueOf(authorization.substring("Bearer test-user-".length()));
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
   }
   ((FilterChain) invocation.getArgument(2)).doFilter((ServletRequest) invocation.getArgument(0), (ServletResponse) invocation.getArgument(1));
   return null;
  }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
 }

 @Test void createRequiresAuthentication() throws Exception { mockMvc.perform(post("/api/itineraries").contentType(MediaType.APPLICATION_JSON).content(validRequest(1))).andExpect(status().isUnauthorized()); }
 @Test void createReturnsExpectedItinerary() throws Exception { Long userId = user(); mockMvc.perform(post("/api/itineraries").header("Authorization", token(userId)).contentType(MediaType.APPLICATION_JSON).content(validRequest(1))).andExpect(status().isCreated()).andExpect(jsonPath("$.itineraryId").isNumber()).andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.bookmarked").value(false)).andExpect(jsonPath("$.region.regionId").value(1)).andExpect(jsonPath("$.startDate").value(START_DATE.toString())).andExpect(jsonPath("$.endDate").value(START_DATE.plusDays(1).toString())).andExpect(jsonPath("$.nights").value(1)).andExpect(jsonPath("$.generationVersion").value(1)).andExpect(jsonPath("$.days").isArray()).andExpect(jsonPath("$.warnings").isArray()); }
 @Test void createReturnsServiceUnavailableWhenTourApiFails() throws Exception { Long userId=user(); places.fail(TourApiProviderException.fromResourceAccessError(new org.springframework.web.client.ResourceAccessException("test",new java.net.SocketTimeoutException()))); Logger logger=(Logger)LoggerFactory.getLogger(ItineraryExceptionHandler.class); ListAppender<ILoggingEvent> logs=new ListAppender<>(); logs.start(); logger.addAppender(logs); try { mockMvc.perform(post("/api/itineraries").header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content(validRequest(1))).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value(503)).andExpect(jsonPath("$.code").value("TOUR_API_UNAVAILABLE")); org.assertj.core.api.Assertions.assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage).contains("tour_api_unavailable type=TIMEOUT causeClass=SocketTimeoutException httpStatus=null").noneMatch(message->message.contains("test-user-")||message.contains("simulated timeout")); } finally { logger.detachAppender(logs); logs.stop(); } }
 @Test void createRejectsInvalidNights() throws Exception { assertCreateError(requestWith("\"nights\":0"), "INVALID_NIGHTS"); assertCreateError(requestWith("\"nights\":8"), "INVALID_NIGHTS"); }
 @Test void createRejectsInvalidPreferenceTags() throws Exception { assertCreateError(requestWith("\"preferenceTags\":[]"), "INVALID_PREFERENCE_TAGS"); assertCreateError(requestWith("\"preferenceTags\":[\"FOOD\",\"NATURE\",\"SEA\",\"WALKING\"]"), "INVALID_PREFERENCE_TAGS"); }
 @Test void createRejectsUnsupportedCompanionType() throws Exception { assertCreateError(requestWith("\"companionType\":\"UNKNOWN\""), "INVALID_COMPANION_TYPE"); }
 @Test void createReturnsNotFoundForUnknownRegion() throws Exception { Long userId=user(); mockMvc.perform(post("/api/itineraries").header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content(validRequest(999L,1))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REGION_NOT_FOUND")); }
 @Test void getAllowsOwnerAndRejectsOtherUserAndUnknownItinerary() throws Exception { Long owner=user(); long itineraryId=create(owner,1); mockMvc.perform(get("/api/itineraries/{id}",itineraryId).header("Authorization",token(owner))).andExpect(status().isOk()); mockMvc.perform(get("/api/itineraries/{id}",itineraryId).header("Authorization",token(user()))).andExpect(status().isForbidden()); mockMvc.perform(get("/api/itineraries/{id}",999999L).header("Authorization",token(owner))).andExpect(status().isNotFound()); }
 @Test void replaceAllowsPlaceAndRejectsFixedItemsAndCompletedItinerary() throws Exception { Long userId=user(); long itineraryId=create(userId,1); var itinerary=itineraries.findDetailById(itineraryId).orElseThrow(); ItineraryItem replaceable=itinerary.getItems().stream().filter(ItineraryItem::isReplaceable).findFirst().orElseThrow(); mockMvc.perform(post("/api/itineraries/{id}/items/{itemId}/replace",itineraryId,replaceable.getId()).header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content(replaceRequest())) .andExpect(status().isOk()); for (ItineraryItemType type : List.of(ItineraryItemType.REST,ItineraryItemType.ARRIVAL,ItineraryItemType.DEPARTURE)) { ItineraryItem item=itinerary.getItems().stream().filter(value->value.getItemType()==type).findFirst().orElseThrow(); mockMvc.perform(post("/api/itineraries/{id}/items/{itemId}/replace",itineraryId,item.getId()).header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content(replaceRequest())) .andExpect(status().isConflict()); } complete(userId,itineraryId); mockMvc.perform(post("/api/itineraries/{id}/items/{itemId}/replace",itineraryId,replaceable.getId()).header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content(replaceRequest())) .andExpect(status().isConflict()); }
 @Test void replaceRejectsFestival() throws Exception { LocalDate start=START_DATE; festivals.setFestivals(List.of(new com.travel.meomulkyung.itinerary.external.FestivalProvider.Festival(9L,"Festival",start,start,null,null))); Long userId=user(); long itineraryId=create(userId,1); ItineraryItem festival=itineraries.findDetailById(itineraryId).orElseThrow().getItems().stream().filter(item->item.getItemType()==ItineraryItemType.FESTIVAL).findFirst().orElseThrow(); mockMvc.perform(post("/api/itineraries/{id}/items/{itemId}/replace",itineraryId,festival.getId()).header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content(replaceRequest())) .andExpect(status().isConflict()); }
 @Test void regenerateIncrementsVersionAndRejectsCompletedItinerary() throws Exception { Long userId=user(); long itineraryId=create(userId,1); mockMvc.perform(post("/api/itineraries/{id}/regenerate",itineraryId).header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content(replaceRequest())) .andExpect(status().isOk()).andExpect(jsonPath("$.generationVersion").value(2)); complete(userId,itineraryId); mockMvc.perform(post("/api/itineraries/{id}/regenerate",itineraryId).header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content(replaceRequest())) .andExpect(status().isConflict()); }
 @Test void bookmarkAndUnbookmarkAreIdempotent() throws Exception { Long userId=user(); long itineraryId=create(userId,1); for(int count=0;count<2;count++) mockMvc.perform(put("/api/itineraries/{id}/bookmark",itineraryId).header("Authorization",token(userId))).andExpect(status().isOk()).andExpect(jsonPath("$.bookmarked").value(true)); for(int count=0;count<2;count++) mockMvc.perform(delete("/api/itineraries/{id}/bookmark",itineraryId).header("Authorization",token(userId))).andExpect(status().isNoContent()); }
 @Test void completionValidatesInputAndRejectsDuplicateCompletion() throws Exception { Long userId=user(); long itineraryId=create(userId,1); mockMvc.perform(post("/api/itineraries/{id}/completion",itineraryId).header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content("{\"stayHours\":24,\"partySize\":2,\"totalSpent\":1000}")) .andExpect(status().isCreated()); mockMvc.perform(post("/api/itineraries/{id}/completion",itineraryId).header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content("{\"stayHours\":24,\"partySize\":2,\"totalSpent\":1000}")) .andExpect(status().isConflict()); assertCompletionBadRequest(userId,"{\"stayHours\":0,\"partySize\":1,\"totalSpent\":0}"); assertCompletionBadRequest(userId,"{\"stayHours\":193,\"partySize\":1,\"totalSpent\":0}"); assertCompletionBadRequest(userId,"{\"stayHours\":1,\"partySize\":0,\"totalSpent\":0}"); assertCompletionBadRequest(userId,"{\"stayHours\":1,\"partySize\":1,\"totalSpent\":-1}"); }
 @Test void weatherUsesDPlus10BoundaryAndProtectsItineraryOwnership() throws Exception { Long userId=user(); long dPlusTenItineraryId=create(userId,LocalDate.now(ItineraryTestConfig.FIXED_CLOCK).plusDays(10),1); mockMvc.perform(get("/api/itineraries/{id}/weather",dPlusTenItineraryId).header("Authorization",token(userId))).andExpect(status().isOk()).andExpect(jsonPath("$.days[0].available").value(true)); long dPlusElevenItineraryId=create(userId,1); mockMvc.perform(get("/api/itineraries/{id}/weather",dPlusElevenItineraryId).header("Authorization",token(userId))).andExpect(status().isOk()).andExpect(jsonPath("$.days[0].available").value(false)); mockMvc.perform(get("/api/itineraries/{id}/weather",dPlusElevenItineraryId).header("Authorization",token(user()))).andExpect(status().isForbidden()); mockMvc.perform(get("/api/itineraries/{id}/weather",999999L).header("Authorization",token(userId))).andExpect(status().isNotFound()); }
 private Long user(){return users.save(ItineraryTestFixture.user()).getId();}
 private long create(Long userId,int nights) throws Exception { return create(userId, START_DATE, nights); }
 private long create(Long userId,LocalDate startDate,int nights) throws Exception { String response=mockMvc.perform(post("/api/itineraries").header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content(validRequest(1L,startDate,nights))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(); return Long.parseLong(response.replaceFirst(".*\\\"itineraryId\\\":(\\d+).*", "$1")); }
 private void complete(Long userId,long itineraryId) throws Exception { mockMvc.perform(post("/api/itineraries/{id}/completion",itineraryId).header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content("{\"stayHours\":24,\"partySize\":2,\"totalSpent\":0}")).andExpect(status().isCreated()); }
 private void assertCompletionBadRequest(Long userId,String content) throws Exception { long itineraryId=create(userId,1); mockMvc.perform(post("/api/itineraries/{id}/completion",itineraryId).header("Authorization",token(userId)).contentType(MediaType.APPLICATION_JSON).content(content)).andExpect(status().isBadRequest()); }
 private void assertCreateError(String content,String code) throws Exception { mockMvc.perform(post("/api/itineraries").header("Authorization",token(user())).contentType(MediaType.APPLICATION_JSON).content(content)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(code)); }
 private String token(Long userId){return "Bearer test-user-"+userId;}
 private String replaceRequest(){return "{\"excludePreviouslyRecommended\":false}";}
 private String validRequest(long regionId,int nights){return validRequest(regionId, START_DATE, nights);}
 private String validRequest(long regionId,LocalDate startDate,int nights){return "{\"regionId\":"+regionId+",\"startDate\":\""+startDate+"\",\"nights\":"+nights+",\"preferenceTags\":[\"FOOD\"],\"companionType\":\"FRIENDS\"}";}
 private String validRequest(int nights){return validRequest(1L,nights);}
 private String requestWith(String replacement){String request=validRequest(1);int separator=replacement.indexOf(':');String field=replacement.substring(0,separator);return request.replace(field+":"+(field.equals("\"preferenceTags\"")?"[\"FOOD\"]":field.equals("\"companionType\"")?"\"FRIENDS\"":"1"),replacement);}
}
