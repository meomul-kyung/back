package com.travel.meomulkyung.itinerary.service;

import com.travel.meomulkyung.itinerary.domain.ItineraryItem;
import com.travel.meomulkyung.itinerary.domain.ItineraryItemType;
import com.travel.meomulkyung.itinerary.dto.PlaceOperationResponses;
import com.travel.meomulkyung.itinerary.external.PlaceDetailProvider;
import com.travel.meomulkyung.itinerary.external.TourApiProviderException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOperationServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 21);

    private final FakeDetailProvider provider = new FakeDetailProvider();
    private final PlaceOperationService service = new PlaceOperationService(null, provider, Clock.systemUTC());

    @Test
    void storedTypeUsesOneCallAndWarnsOnClosedDay() {
        provider.hours = new PlaceDetailProvider.OperationHours("09:00~18:00", "매주 월요일");

        PlaceOperationResponses.OperationInfo info = service.operationInfo(item(ItineraryItemType.TOURIST_SPOT, 1201L, "12"), MONDAY);

        assertThat(provider.calls).containsExactly("intro:1201:12");
        assertThat(info.status()).isEqualTo("OK");
        assertThat(info.contentTypeId()).isEqualTo("12");
        assertThat(info.visitDayOfWeek()).isEqualTo("MONDAY");
        assertThat(info.useTime()).isEqualTo("09:00~18:00");
        assertThat(info.closedDayWarning()).isNotNull();
        assertThat(info.closedDayWarning().level()).isEqualTo("POSSIBLE_CLOSED");
        assertThat(info.closedDayWarning().message()).contains("(월)");
        assertThat(info.source()).isEqualTo("ⓒ한국관광공사");
        assertThat(info.notice()).isNotBlank();
        assertThat(info.fetchedAt()).isNotNull();
    }

    @Test
    void noWarningWhenVisitDayIsOpen() {
        provider.hours = new PlaceDetailProvider.OperationHours("09:00~18:00", "매주 월요일");

        PlaceOperationResponses.OperationInfo info = service.operationInfo(
                item(ItineraryItemType.TOURIST_SPOT, 1201L, "12"), MONDAY.plusDays(1));

        assertThat(info.closedDayWarning()).isNull();
    }

    @Test
    void legacyRestaurantSkipsTypeLookup() {
        service.operationInfo(item(ItineraryItemType.RESTAURANT, 3901L, null), MONDAY);

        assertThat(provider.calls).containsExactly("intro:3901:39");
    }

    @Test
    void legacyTouristSpotLooksUpTypeFirst() {
        provider.commonType = "14";

        PlaceOperationResponses.OperationInfo info = service.operationInfo(item(ItineraryItemType.TOURIST_SPOT, 1401L, null), MONDAY);

        assertThat(provider.calls).containsExactly("common:1401", "intro:1401:14");
        assertThat(info.contentTypeId()).isEqualTo("14");
    }

    @Test
    void shoppingAndExperienceAreNotSupportedWithoutIntroCall() {
        provider.commonType = "38";
        PlaceOperationResponses.OperationInfo shopping = service.operationInfo(item(ItineraryItemType.TOURIST_SPOT, 3801L, null), MONDAY);
        PlaceOperationResponses.OperationInfo experience = service.operationInfo(item(ItineraryItemType.EXPERIENCE, 2801L, null), MONDAY);

        assertThat(shopping.status()).isEqualTo("NOT_SUPPORTED");
        assertThat(experience.status()).isEqualTo("NOT_SUPPORTED");
        assertThat(provider.calls).containsExactly("common:3801");
    }

    @Test
    void nonPlaceItemsAreNotSupported() {
        ItineraryItem arrival = new ItineraryItem(null, 1, 1, ItineraryItemType.ARRIVAL, null, "ARRIVAL");
        ItineraryItem festival = new ItineraryItem(null, 1, 2, 1501L, "축제", null, null, MONDAY, MONDAY);

        assertThat(service.operationInfo(arrival, MONDAY).status()).isEqualTo("NOT_SUPPORTED");
        assertThat(service.operationInfo(festival, MONDAY).status()).isEqualTo("NOT_SUPPORTED");
        assertThat(provider.calls).isEmpty();
    }

    @Test
    void providerFailureIsUnavailableInsteadOfError() {
        provider.failure = new TourApiProviderException("TourAPI returned HTTP status 500.");

        PlaceOperationResponses.OperationInfo info = service.operationInfo(item(ItineraryItemType.RESTAURANT, 3901L, null), MONDAY);

        assertThat(info.status()).isEqualTo("UNAVAILABLE");
        assertThat(info.useTime()).isNull();
        assertThat(info.closedDayWarning()).isNull();
    }

    @Test
    void emptyFieldsAreNoData() {
        provider.hours = new PlaceDetailProvider.OperationHours(null, null);

        assertThat(service.operationInfo(item(ItineraryItemType.RESTAURANT, 3901L, null), MONDAY).status())
                .isEqualTo("NO_DATA");
    }

    private static ItineraryItem item(ItineraryItemType type, Long contentId, String contentTypeId) {
        return new ItineraryItem(null, 1, 2, type, contentId, "장소", null, null, 36.5, 128.7, contentTypeId);
    }

    private static class FakeDetailProvider implements PlaceDetailProvider {
        final List<String> calls = new ArrayList<>();
        OperationHours hours = new OperationHours("10:00~20:00", null);
        String commonType;
        RuntimeException failure;

        @Override
        public Optional<String> contentTypeId(long contentId) {
            calls.add("common:" + contentId);
            if (failure != null) throw failure;
            return Optional.ofNullable(commonType);
        }

        @Override
        public OperationHours operationHours(long contentId, String contentTypeId) {
            calls.add("intro:" + contentId + ":" + contentTypeId);
            if (failure != null) throw failure;
            return hours;
        }
    }
}
