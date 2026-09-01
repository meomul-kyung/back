package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles;
import com.travel.meomulkyung.region.domain.Region;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TourApiRegionCodeConfigurationTest {
    private static final Map<Long, String> OFFICIAL_SIGUNGU_CODES = Map.ofEntries(
            Map.entry(1L, "11"), Map.entry(2L, "14"), Map.entry(3L, "7"), Map.entry(4L, "9"),
            Map.entry(5L, "8"), Map.entry(6L, "13"), Map.entry(7L, "21"), Map.entry(8L, "19"),
            Map.entry(9L, "20"), Map.entry(10L, "16"), Map.entry(11L, "18"), Map.entry(12L, "12"),
            Map.entry(13L, "3"), Map.entry(14L, "10"), Map.entry(15L, "17"));

    @Test
    void everySeededOperatingRegionHasAMapping() throws IOException {
        TourApiProperties properties = properties();

        assertThat(properties.getRegionCodes().keySet())
                .containsExactlyInAnyOrderElementsOf(RegionRecommendationProfiles.ALL.stream()
                        .map(RegionRecommendationProfiles.RegionRecommendationProfile::regionId).toList());
    }

    @Test
    void everyOperatingRegionUsesGyeongbukAreaCode() throws IOException {
        assertThat(properties().getRegionCodes().values())
                .extracting(TourApiProperties.RegionCode::getAreaCode)
                .containsOnly("35");
    }

    @Test
    void everyOperatingRegionUsesTheOfficialSigunguCode() throws IOException {
        Map<Long, String> actual = new LinkedHashMap<>();
        properties().getRegionCodes().forEach((regionId, code) -> actual.put(regionId, code.getSigunguCode()));

        assertThat(actual).isEqualTo(OFFICIAL_SIGUNGU_CODES);
    }

    @Test
    void andongUsesTheOfficialGyeongbukAndAndongCodes() throws IOException {
        TourApiProperties.RegionCode andong = properties().getRegionCodes().get(1L);

        assertThat(andong.getAreaCode()).isEqualTo("35");
        assertThat(andong.getSigunguCode()).isEqualTo("11");
    }

    @Test
    void undefinedRegionIdStillRaisesTheExistingMappingError() throws IOException {
        TourApiProperties properties = requestProperties();
        TourApiPlaceProvider provider = new TourApiPlaceProvider(RestClient.create(), new ObjectMapper(), properties);

        assertThatThrownBy(() -> provider.findPlaces(region(99L)))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI region mapping is missing.");
    }

    @Test
    void placeProviderSendsTheConfiguredAreaAndSigunguCodes() throws IOException {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiPlaceProvider provider = new TourApiPlaceProvider(builder.build(), new ObjectMapper(), requestProperties());
        server.expect(request -> assertThat(request.getURI().getRawQuery()).contains("areaCode=35", "sigunguCode=11"))
                .andRespond(withSuccess(emptyItemsJson(), MediaType.APPLICATION_JSON));

        assertThat(provider.findPlaces(region(1L))).isEmpty();
        server.verify();
    }

    @Test
    void festivalProviderSendsTheSameConfiguredAreaAndSigunguCodes() throws IOException {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiFestivalProvider provider = new TourApiFestivalProvider(builder.build(), new ObjectMapper(), requestProperties());
        server.expect(request -> assertThat(request.getURI().getRawQuery()).contains("areaCode=35", "sigunguCode=11"))
                .andRespond(withSuccess(emptyItemsJson(), MediaType.APPLICATION_JSON));

        assertThat(provider.findFestivals(region(1L), LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12))).isEmpty();
        server.verify();
    }

    @Test
    void serviceKeyRemainsAnEnvironmentPlaceholderRatherThanAConfiguredValue() throws IOException {
        assertThat(rawProperties().getProperty("tour-api.service-key"))
                .isEqualTo("${TOUR_API_SERVICE_KEY:}");
    }

    private TourApiProperties requestProperties() throws IOException {
        TourApiProperties properties = properties();
        properties.setBaseUrl("https://tour-api.example/KorService2");
        properties.setServiceKey("encoded%2Ftest-key");
        return properties;
    }

    private TourApiProperties properties() throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        rawProperties().forEach((key, value) -> values.put((String) key, value));
        return new Binder(java.util.List.of(new MapConfigurationPropertySource(values)))
                .bind("tour-api", Bindable.of(TourApiProperties.class))
                .orElseThrow(() -> new IllegalStateException("TourAPI properties did not bind."));
    }

    private Properties rawProperties() throws IOException {
        Properties properties = new Properties();
        try (var input = new ClassPathResource("application.properties").getInputStream()) {
            properties.load(input);
        }
        return properties;
    }

    private Region region(Long id) {
        return new Region(id, "Test region", null, null, "identity", null);
    }

    private String emptyItemsJson() {
        return """
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{}}}}
                """;
    }
}
