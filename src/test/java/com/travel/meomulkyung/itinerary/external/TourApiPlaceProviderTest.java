package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.meomulkyung.itinerary.ItineraryTestConfig;
import com.travel.meomulkyung.itinerary.domain.ItineraryItemType;
import com.travel.meomulkyung.region.domain.Region;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TourApiPlaceProviderTest {
    private static final String ENCODED_SERVICE_KEY = "encoded%2Fservice%2Bkey";

    @Test
    void mapsTourApiFieldsAndKeepsAnEncodedServiceKeyUnchanged() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY);
        testProvider.server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/KorService2/areaBasedList2");
            String query = request.getURI().getRawQuery();
            assertThat(query).contains("serviceKey=" + ENCODED_SERVICE_KEY)
                    .doesNotContain("%252F")
                    .contains("MobileOS=ETC", "MobileApp=meomul-kyung", "_type=json", "pageNo=1", "numOfRows=50",
                            "areaCode=1", "sigunguCode=2")
                    .doesNotContain("contentTypeId=");
        }).andRespond(withSuccess(successItemsJson(), MediaType.APPLICATION_JSON));

        List<TourPlaceProvider.Place> places = testProvider.provider.findPlaces(region());

        assertThat(places).containsExactly(
                new TourPlaceProvider.Place(101L, "Place A", ItineraryItemType.TOURIST_SPOT,
                        "https://image.example/a.jpg", "Address A Detail", 37.1234, 127.5678),
                new TourPlaceProvider.Place(102L, "Place B", ItineraryItemType.RESTAURANT,
                        "https://image.example/b-thumb.jpg", "Address B", 35.1, 129.2));
        testProvider.server.verify();
    }

    @Test
    void returnsEmptyListWhenItemsAreMissing() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY);
        testProvider.server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":""}}}
                """, MediaType.APPLICATION_JSON));

        assertThat(testProvider.provider.findPlaces(region())).isEmpty();
    }

    @Test
    void handlesOneItemObject() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY);
        testProvider.server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":
                {"contentid":"301","contenttypeid":"28","title":"Experience"}}}}}
                """, MediaType.APPLICATION_JSON));

        assertThat(testProvider.provider.findPlaces(region())).containsExactly(
                new TourPlaceProvider.Place(301L, "Experience", ItineraryItemType.EXPERIENCE,
                        null, null, null, null));
    }

    @Test
    void returnsOnlyAllowedContentTypesWithExpectedItemTypes() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY);
        testProvider.server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[
                {"contentid":"12","contenttypeid":"12","title":"Tourist spot"},
                {"contentid":"14","contenttypeid":"14","title":"Culture"},
                {"contentid":"28","contenttypeid":"28","title":"Experience"},
                {"contentid":"38","contenttypeid":"38","title":"Shopping"},
                {"contentid":"39","contenttypeid":"39","title":"Restaurant"},
                {"contentid":"15","contenttypeid":"15","title":"Festival"},
                {"contentid":"25","contenttypeid":"25","title":"Course"},
                {"contentid":"32","contenttypeid":"32","title":"Accommodation"},
                {"contentid":"40","contenttypeid":"40","title":"Unknown"},
                {"contentid":"41","contenttypeid":"","title":"Blank"},
                {"contentid":"42","contenttypeid":"not-a-number","title":"Invalid"}
                ]}}}}
                """, MediaType.APPLICATION_JSON));

        assertThat(testProvider.provider.findPlaces(region())).containsExactly(
                new TourPlaceProvider.Place(12L, "Tourist spot", ItineraryItemType.TOURIST_SPOT, null, null, null, null),
                new TourPlaceProvider.Place(14L, "Culture", ItineraryItemType.TOURIST_SPOT, null, null, null, null),
                new TourPlaceProvider.Place(28L, "Experience", ItineraryItemType.EXPERIENCE, null, null, null, null),
                new TourPlaceProvider.Place(38L, "Shopping", ItineraryItemType.TOURIST_SPOT, null, null, null, null),
                new TourPlaceProvider.Place(39L, "Restaurant", ItineraryItemType.RESTAURANT, null, null, null, null));
    }

    @Test
    void returnsEmptyListWhenResponseContainsOnlyAccommodations() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY);
        testProvider.server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[
                {"contentid":"3201","contenttypeid":"32","title":"Accommodation A"},
                {"contentid":"3202","contenttypeid":"32","title":"Accommodation B"}
                ]}}}}
                """, MediaType.APPLICATION_JSON));

        assertThat(testProvider.provider.findPlaces(region())).isEmpty();
    }

    @Test
    void rejectsTourApiApplicationErrorWithoutLeakingTheServiceKey() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY);
        testProvider.server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0301","resultMsg":"denied"},"body":{}}}
                """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> testProvider.provider.findPlaces(region()))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI returned an application error.")
                .hasMessageNotContaining(ENCODED_SERVICE_KEY);
    }

    @Test
    void handlesHttp5xxWithoutLeakingTheServiceKey() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY);
        testProvider.server.expect(anything()).andRespond(withServerError());

        assertThatThrownBy(() -> testProvider.provider.findPlaces(region()))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI returned HTTP status 500.")
                .hasCauseInstanceOf(org.springframework.web.client.RestClientResponseException.class)
                .hasMessageNotContaining(ENCODED_SERVICE_KEY);
    }

    @Test
    void handlesCommunicationFailureWithoutLeakingTheServiceKey() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY);
        testProvider.server.expect(anything()).andRespond(request -> {
            throw new ResourceAccessException("simulated timeout", new java.net.SocketTimeoutException("simulated timeout"));
        });

        assertThatThrownBy(() -> testProvider.provider.findPlaces(region()))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI request timed out (SocketTimeoutException).")
                .hasCauseInstanceOf(ResourceAccessException.class)
                .hasRootCauseInstanceOf(java.net.SocketTimeoutException.class)
                .hasMessageNotContaining(ENCODED_SERVICE_KEY);
    }

    @Test
    void classifiesConnectionFailureWithoutLeakingTheServiceKey() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY);
        testProvider.server.expect(anything()).andRespond(request -> {
            throw new ResourceAccessException("simulated connection failure", new java.net.ConnectException("simulated connection failure"));
        });

        assertThatThrownBy(() -> testProvider.provider.findPlaces(region()))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI connection failed (ConnectException).")
                .hasCauseInstanceOf(ResourceAccessException.class)
                .hasRootCauseInstanceOf(java.net.ConnectException.class)
                .hasMessageNotContaining(ENCODED_SERVICE_KEY);
    }

    @Test
    void handlesInvalidJsonWithoutLeakingTheServiceKey() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY);
        testProvider.server.expect(anything()).andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> testProvider.provider.findPlaces(region()))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI returned an invalid JSON response.")
                .hasCauseInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class)
                .hasMessageNotContaining(ENCODED_SERVICE_KEY);
    }

    @Test
    void rejectsMissingServiceKeyBeforeCallingTourApi() {
        TestProvider testProvider = provider("");

        assertThatThrownBy(() -> testProvider.provider.findPlaces(region()))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI service key is not configured.");
    }

    @Test
    void rejectsMissingRegionMappingBeforeCallingTourApi() {
        TourApiProperties properties = properties(ENCODED_SERVICE_KEY);
        properties.setRegionCodes(Map.of());
        TourApiPlaceProvider provider = new TourApiPlaceProvider(RestClient.create(), new ObjectMapper(), properties);

        assertThatThrownBy(() -> provider.findPlaces(region()))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI region mapping is missing.");
    }

    @Test
    void testConfigurationSelectsTheFakeProvider() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(TourApiConfiguration.class, ItineraryTestConfig.class);
            context.refresh();

            assertThat(context.getBean(TourPlaceProvider.class))
                    .isNotInstanceOf(TourApiPlaceProvider.class);
        }
    }

    private TestProvider provider(String serviceKey) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new TestProvider(new TourApiPlaceProvider(builder.build(), new ObjectMapper(), properties(serviceKey)), server);
    }

    private TourApiProperties properties(String serviceKey) {
        TourApiProperties properties = new TourApiProperties();
        properties.setBaseUrl("https://tour-api.example/KorService2");
        properties.setServiceKey(serviceKey);
        properties.setRegionCodes(Map.of(1L, new TourApiProperties.RegionCode("1", "2")));
        return properties;
    }

    private Region region() {
        return new Region(1L, "Test region", null, null, "identity", null);
    }

    private String successItemsJson() {
        return """
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[
                {"contentid":"101","contenttypeid":"12","title":"Place A","addr1":"Address A","addr2":"Detail","firstimage":"https://image.example/a.jpg","mapy":"37.1234","mapx":"127.5678"},
                {"contentid":"102","contenttypeid":"39","title":"Place B","addr1":"Address B","firstimage2":"https://image.example/b-thumb.jpg","mapy":"35.1","mapx":"129.2"}
                ]}}}}
                """;
    }

    private record TestProvider(TourApiPlaceProvider provider, MockRestServiceServer server) { }
}
