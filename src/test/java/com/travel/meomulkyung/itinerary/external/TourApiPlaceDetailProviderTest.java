package com.travel.meomulkyung.itinerary.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TourApiPlaceDetailProviderTest {

    private static final String ENCODED_SERVICE_KEY = "encoded%2Fservice%2Bkey";

    private MockRestServiceServer server;

    private TourApiPlaceDetailProvider provider(String serviceKey) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        TourApiProperties properties = new TourApiProperties();
        properties.setBaseUrl("https://tour-api.example/KorService2");
        properties.setServiceKey(serviceKey);
        return new TourApiPlaceDetailProvider(builder.build(), new ObjectMapper(), properties);
    }

    @Test
    void restaurantUsesFoodFieldsAndCleansLineBreaks() {
        TourApiPlaceDetailProvider provider = provider(ENCODED_SERVICE_KEY);
        server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/KorService2/detailIntro2");
            assertThat(request.getURI().getRawQuery())
                    .contains("contentId=2001", "contentTypeId=39", "serviceKey=" + ENCODED_SERVICE_KEY, "_type=json")
                    .doesNotContain("%252F");
        }).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[
                {"contentid":"2001","opentimefood":"11:00~21:00<br>(브레이크타임 15:00~17:00)","restdatefood":"매주 월요일<br />설·추석 당일",
                 "usetime":"should-not-be-used"}]}}}}
                """, MediaType.APPLICATION_JSON));

        PlaceDetailProvider.OperationHours hours = provider.operationHours(2001L, "39");

        server.verify();
        assertThat(hours.useTime()).isEqualTo("11:00~21:00\n(브레이크타임 15:00~17:00)");
        assertThat(hours.restDate()).isEqualTo("매주 월요일\n설·추석 당일");
    }

    @Test
    void cultureFacilityUsesCultureFields() {
        TourApiPlaceDetailProvider provider = provider(ENCODED_SERVICE_KEY);
        server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":
                {"usetimeculture":"09:00~18:00","restdateculture":"매주 월요일"}}}}}
                """, MediaType.APPLICATION_JSON));

        PlaceDetailProvider.OperationHours hours = provider.operationHours(1401L, "14");

        assertThat(hours.useTime()).isEqualTo("09:00~18:00");
        assertThat(hours.restDate()).isEqualTo("매주 월요일");
    }

    @Test
    void blankFieldsBecomeNull() {
        TourApiPlaceDetailProvider provider = provider(ENCODED_SERVICE_KEY);
        server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{"usetime":"","restdate":" <br> "}]}}}}
                """, MediaType.APPLICATION_JSON));

        PlaceDetailProvider.OperationHours hours = provider.operationHours(1201L, "12");

        assertThat(hours.useTime()).isNull();
        assertThat(hours.restDate()).isNull();
    }

    @Test
    void unsupportedTypeDoesNotCallTheApi() {
        TourApiPlaceDetailProvider provider = provider(ENCODED_SERVICE_KEY);

        PlaceDetailProvider.OperationHours hours = provider.operationHours(2801L, "28");

        server.verify();
        assertThat(hours.useTime()).isNull();
        assertThat(hours.restDate()).isNull();
        assertThat(TourApiPlaceDetailProvider.supports("38")).isFalse();
        assertThat(TourApiPlaceDetailProvider.supports(null)).isFalse();
    }

    @Test
    void commonInfoReturnsContentTypeId() {
        TourApiPlaceDetailProvider provider = provider(ENCODED_SERVICE_KEY);
        server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/KorService2/detailCommon2");
            assertThat(request.getURI().getRawQuery()).contains("contentId=1401").doesNotContain("contentTypeId=");
        }).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{"contentid":"1401","contenttypeid":"14"}]}}}}
                """, MediaType.APPLICATION_JSON));

        assertThat(provider.contentTypeId(1401L)).contains("14");
    }

    @Test
    void emptyItemsReturnNothing() {
        TourApiPlaceDetailProvider provider = provider(ENCODED_SERVICE_KEY);
        server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0000"},"body":{"items":""}}}
                """, MediaType.APPLICATION_JSON));

        assertThat(provider.contentTypeId(9L)).isEmpty();
    }

    @Test
    void applicationErrorAndHttpErrorAreProviderExceptionsWithoutLeakingTheKey() {
        TourApiPlaceDetailProvider provider = provider(ENCODED_SERVICE_KEY);
        server.expect(anything()).andRespond(withSuccess("""
                {"response":{"header":{"resultCode":"0301","resultMsg":"denied"}}}
                """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> provider.operationHours(1L, "12"))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessageNotContaining(ENCODED_SERVICE_KEY);

        TourApiPlaceDetailProvider failing = provider(ENCODED_SERVICE_KEY);
        server.expect(anything()).andRespond(withServerError());
        assertThatThrownBy(() -> failing.contentTypeId(1L))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI returned HTTP status 500.");
    }

    @Test
    void missingServiceKeyFailsBeforeCalling() {
        TourApiPlaceDetailProvider provider = provider("");

        assertThatThrownBy(() -> provider.operationHours(1L, "12"))
                .isInstanceOf(TourApiProviderException.class)
                .hasMessage("TourAPI service key is not configured.");
        server.verify();
    }
}
