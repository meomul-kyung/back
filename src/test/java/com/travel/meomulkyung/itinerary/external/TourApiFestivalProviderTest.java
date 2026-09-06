package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.meomulkyung.itinerary.ItineraryTestConfig;
import com.travel.meomulkyung.region.domain.Region;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TourApiFestivalProviderTest {
    private static final String ENCODED_SERVICE_KEY = "encoded%2Ffestival%2Bkey";
    private static final LocalDate START = LocalDate.of(2026, 9, 10);
    private static final LocalDate END = LocalDate.of(2026, 9, 12);

    @Test
    void mapsFestivalArrayFields() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY, "2");
        testProvider.server.expect(anything()).andRespond(withSuccess(festivalArrayJson(), MediaType.APPLICATION_JSON));

        assertThat(testProvider.provider.findFestivals(region(), START, END)).containsExactly(
                new FestivalProvider.Festival(101L, "Festival A", LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 13),
                        "https://image.example/a.jpg", "Address A Detail"),
                new FestivalProvider.Festival(102L, "Festival B", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10),
                        "https://image.example/b-thumb.jpg", "Address B"));
    }

    @Test
    void mapsActualFestivalItemWithBlankAreaAndSigunguCodes() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY, "2");
        testProvider.server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":
                {"contentid":"4090201","contenttypeid":"15","title":"가든 나이트 마켓","addr1":"울산광역시 남구 대공원로 94 (옥동)","addr2":"","eventstartdate":"20260729","eventenddate":"20260829","firstimage":"https://tong.visitkorea.or.kr/cms/resource/02/4090202_image2_1.jpg","firstimage2":"https://tong.visitkorea.or.kr/cms/resource/02/4090202_image3_1.jpg","mapx":"129.2938457635","mapy":"35.5310582726","areacode":"","sigungucode":""}
                }}}}
                """, MediaType.APPLICATION_JSON));

        assertThat(testProvider.provider.findFestivals(region(), START, END)).containsExactly(
                new FestivalProvider.Festival(4090201L, "가든 나이트 마켓", LocalDate.of(2026, 7, 29), LocalDate.of(2026, 8, 29),
                        "https://tong.visitkorea.or.kr/cms/resource/02/4090202_image2_1.jpg", "울산광역시 남구 대공원로 94 (옥동)"));
    }

    @Test
    void handlesOneFestivalObject() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY, "2");
        testProvider.server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":
                {"contentid":"301","title":"One Festival","eventstartdate":"20260910","eventenddate":"20260911"}}}}}
                """, MediaType.APPLICATION_JSON));

        assertThat(testProvider.provider.findFestivals(region(), START, END)).containsExactly(
                new FestivalProvider.Festival(301L, "One Festival", START, LocalDate.of(2026, 9, 11), null, null));
    }

    @Test
    void returnsEmptyListWhenItemsAreMissing() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY, "2");
        testProvider.server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":""}}}
                """, MediaType.APPLICATION_JSON));

        assertThat(testProvider.provider.findFestivals(region(), START, END)).isEmpty();
    }

    @Test
    void sendsDatesRegionCodesAndAnUnchangedEncodedServiceKey() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY, "2");
        testProvider.server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/KorService2/searchFestival2");
            String query = request.getURI().getRawQuery();
            assertThat(query).contains("eventStartDate=20260910", "eventEndDate=20260912", "areaCode=1", "sigunguCode=2",
                            "MobileOS=ETC", "MobileApp=meomul-kyung", "_type=json", "pageNo=1", "numOfRows=50",
                            "serviceKey=" + ENCODED_SERVICE_KEY)
                    .doesNotContain("%252F");
        }).andRespond(withSuccess(emptyItemsJson(), MediaType.APPLICATION_JSON));

        testProvider.provider.findFestivals(region(), START, END);
        testProvider.server.verify();
    }

    @Test
    void omitsSigunguCodeWhenNoMappingIsConfigured() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY, null);
        testProvider.server.expect(request -> assertThat(request.getURI().getRawQuery()).doesNotContain("sigunguCode"))
                .andRespond(withSuccess(emptyItemsJson(), MediaType.APPLICATION_JSON));

        assertThat(testProvider.provider.findFestivals(region(), START, END)).isEmpty();
    }

    @Test
    void skipsInvalidFestivalDatesAndKeepsValidFestivals() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY, "2");
        testProvider.server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[
                {"contentid":"401","title":"Invalid","eventstartdate":"bad-date","eventenddate":"20260912"},
                {"contentid":"402","title":"Valid","eventstartdate":"20260910","eventenddate":"20260912"}
                ]}}}}
                """, MediaType.APPLICATION_JSON));

        assertThat(testProvider.provider.findFestivals(region(), START, END)).containsExactly(
                new FestivalProvider.Festival(402L, "Valid", START, END, null, null));
    }

    @Test
    void rejectsTourApiApplicationErrorWithoutLeakingTheServiceKey() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY, "2");
        testProvider.server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0301"},"body":{}}}
                """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> testProvider.provider.findFestivals(region(), START, END))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI returned an application error.")
                .hasMessageNotContaining(ENCODED_SERVICE_KEY);
    }

    @Test
    void handlesHttp5xxWithoutLeakingTheServiceKey() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY, "2");
        testProvider.server.expect(anything()).andRespond(withServerError());

        assertThatThrownBy(() -> testProvider.provider.findFestivals(region(), START, END))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI returned an HTTP error.")
                .hasMessageNotContaining(ENCODED_SERVICE_KEY);
    }

    @Test
    void handlesCommunicationFailureWithoutLeakingTheServiceKey() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY, "2");
        testProvider.server.expect(anything()).andRespond(request -> {
            throw new ResourceAccessException("simulated timeout");
        });

        assertThatThrownBy(() -> testProvider.provider.findFestivals(region(), START, END))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI request failed.")
                .hasMessageNotContaining(ENCODED_SERVICE_KEY);
    }

    @Test
    void handlesInvalidJsonWithoutLeakingTheServiceKey() {
        TestProvider testProvider = provider(ENCODED_SERVICE_KEY, "2");
        testProvider.server.expect(anything()).andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> testProvider.provider.findFestivals(region(), START, END))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI returned an invalid JSON response.")
                .hasMessageNotContaining(ENCODED_SERVICE_KEY);
    }

    @Test
    void rejectsMissingServiceKeyBeforeCallingTourApi() {
        TestProvider testProvider = provider("", "2");

        assertThatThrownBy(() -> testProvider.provider.findFestivals(region(), START, END))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI service key is not configured.");
    }

    @Test
    void rejectsMissingRegionMappingBeforeCallingTourApi() {
        TourApiProperties properties = properties(ENCODED_SERVICE_KEY, "2");
        properties.setRegionCodes(Map.of());
        TourApiFestivalProvider provider = new TourApiFestivalProvider(RestClient.create(), new ObjectMapper(), properties);

        assertThatThrownBy(() -> provider.findFestivals(region(), START, END))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI region mapping is missing.");
    }

    @Test
    void testConfigurationSelectsTheFakeFestivalProvider() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(TourApiConfiguration.class, ItineraryTestConfig.class);
            context.refresh();

            assertThat(context.getBean(FestivalProvider.class))
                    .isInstanceOf(ItineraryTestConfig.FakeFestivalProvider.class);
        }
    }

    private TestProvider provider(String serviceKey, String sigunguCode) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new TestProvider(new TourApiFestivalProvider(builder.build(), new ObjectMapper(), properties(serviceKey, sigunguCode)), server);
    }

    private TourApiProperties properties(String serviceKey, String sigunguCode) {
        TourApiProperties properties = new TourApiProperties();
        properties.setBaseUrl("https://tour-api.example/KorService2");
        properties.setServiceKey(serviceKey);
        properties.setRegionCodes(Map.of(1L, new TourApiProperties.RegionCode("1", sigunguCode)));
        return properties;
    }

    private Region region() {
        return new Region(1L, "Test region", null, null, "identity", null);
    }

    private String emptyItemsJson() {
        return """
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{}}}}
                """;
    }

    private String festivalArrayJson() {
        return """
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[
                {"contentid":"101","title":"Festival A","addr1":"Address A","addr2":"Detail","firstimage":"https://image.example/a.jpg","eventstartdate":"20260909","eventenddate":"20260913","mapy":"37.1","mapx":"127.1"},
                {"contentid":"102","title":"Festival B","addr1":"Address B","firstimage2":"https://image.example/b-thumb.jpg","eventstartdate":"20260910","eventenddate":"20260910","mapy":"35.1","mapx":"129.2"}
                ]}}}}
                """;
    }

    private record TestProvider(TourApiFestivalProvider provider, MockRestServiceServer server) { }
}
