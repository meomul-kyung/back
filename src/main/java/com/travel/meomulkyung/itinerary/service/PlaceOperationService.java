package com.travel.meomulkyung.itinerary.service;

import com.travel.meomulkyung.itinerary.domain.Itinerary;
import com.travel.meomulkyung.itinerary.domain.ItineraryItem;
import com.travel.meomulkyung.itinerary.domain.ItineraryItemType;
import com.travel.meomulkyung.itinerary.dto.PlaceOperationResponses;
import com.travel.meomulkyung.itinerary.external.PlaceDetailProvider;
import com.travel.meomulkyung.itinerary.external.TourApiPlaceDetailProvider;
import com.travel.meomulkyung.itinerary.external.TourApiProviderException;
import com.travel.meomulkyung.itinerary.repository.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

/**
 * 일정 항목의 운영시간·휴무일을 TourAPI에서 실시간 조회한다. (저장하지 않음)
 *
 * <p>항목에 저장된 contentTypeId가 있으면 1콜, 없는 기존 항목은 공통정보로 타입을 먼저 확인해 2콜이다.
 * 음식점·체험은 타입이 정해져 있어 추가 조회 없이 처리한다.
 * 외부 호출은 DB 트랜잭션 밖에서 수행한다. (일정은 fetch join으로 한 번에 읽는다)
 */
@Service
@RequiredArgsConstructor
public class PlaceOperationService {

    private static final Logger log = LoggerFactory.getLogger(PlaceOperationService.class);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Set<ItineraryItemType> PLACE_TYPES =
            Set.of(ItineraryItemType.TOURIST_SPOT, ItineraryItemType.RESTAURANT, ItineraryItemType.EXPERIENCE);
    private static final Map<DayOfWeek, String> KOREAN_DAYS = Map.of(
            DayOfWeek.MONDAY, "월", DayOfWeek.TUESDAY, "화", DayOfWeek.WEDNESDAY, "수", DayOfWeek.THURSDAY, "목",
            DayOfWeek.FRIDAY, "금", DayOfWeek.SATURDAY, "토", DayOfWeek.SUNDAY, "일");
    static final String NOTICE = "운영시간·휴무일은 현장 사정에 따라 달라질 수 있으니 방문 전 확인하세요.";
    static final String SOURCE = "ⓒ한국관광공사";

    private final ItineraryRepository itineraries;
    private final PlaceDetailProvider placeDetailProvider;
    private final Clock clock;

    public PlaceOperationResponses.OperationInfo operationInfo(Long userId, Long itineraryId, Long itemId) {
        Itinerary itinerary = itineraries.findDetailById(itineraryId).orElseThrow(() ->
                new ItineraryException(HttpStatus.NOT_FOUND, "ITINERARY_NOT_FOUND", "일정을 찾을 수 없습니다."));
        if (!itinerary.getUser().getId().equals(userId)) {
            throw new ItineraryException(HttpStatus.FORBIDDEN, "ITINERARY_ACCESS_DENIED", "일정 접근 권한이 없습니다.");
        }
        ItineraryItem item = itinerary.getItems().stream()
                .filter(value -> value.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ItineraryException(HttpStatus.NOT_FOUND, "ITINERARY_ITEM_NOT_FOUND", "일정 항목을 찾을 수 없습니다."));
        return operationInfo(item, itinerary.getStartDate().plusDays(item.getDayNumber() - 1L));
    }

    PlaceOperationResponses.OperationInfo operationInfo(ItineraryItem item, LocalDate visitDate) {
        if (item.getContentId() == null || !PLACE_TYPES.contains(item.getItemType())) {
            return response(item, visitDate, null, "NOT_SUPPORTED", null, null);
        }
        String contentTypeId;
        PlaceDetailProvider.OperationHours hours;
        try {
            contentTypeId = contentTypeId(item);
            if (!TourApiPlaceDetailProvider.supports(contentTypeId)) {
                return response(item, visitDate, contentTypeId, "NOT_SUPPORTED", null, null);
            }
            hours = placeDetailProvider.operationHours(item.getContentId(), contentTypeId);
        } catch (TourApiProviderException exception) {
            log.warn("place_operation_unavailable itemId={} type={} causeClass={} httpStatus={}", item.getId(),
                    exception.getFailureType(), exception.getCauseClassName(), exception.getHttpStatusCode());
            return response(item, visitDate, item.getContentTypeId(), "UNAVAILABLE", null, null);
        }
        if (hours.useTime() == null && hours.restDate() == null) {
            return response(item, visitDate, contentTypeId, "NO_DATA", null, null);
        }
        return response(item, visitDate, contentTypeId, "OK", hours.useTime(), hours.restDate());
    }

    private String contentTypeId(ItineraryItem item) {
        if (item.getContentTypeId() != null) {
            return item.getContentTypeId();
        }
        if (item.getItemType() == ItineraryItemType.RESTAURANT) {
            return "39";
        }
        if (item.getItemType() == ItineraryItemType.EXPERIENCE) {
            return "28";
        }
        // 기존 관광지 항목은 관광지(12)·문화시설(14)·쇼핑(38)이 섞여 있어 공통정보로 확인한다.
        return placeDetailProvider.contentTypeId(item.getContentId()).orElse(null);
    }

    private PlaceOperationResponses.OperationInfo response(ItineraryItem item, LocalDate visitDate, String contentTypeId,
                                                           String status, String useTime, String restDate) {
        PlaceOperationResponses.ClosedDayWarning warning = ClosedDayDetector.mayBeClosed(restDate, visitDate)
                ? new PlaceOperationResponses.ClosedDayWarning("POSSIBLE_CLOSED",
                "방문일(" + KOREAN_DAYS.get(visitDate.getDayOfWeek()) + ")이 휴무일일 수 있습니다. 방문 전 확인하세요.")
                : null;
        return new PlaceOperationResponses.OperationInfo(
                item.getId(),
                item.getContentId(),
                contentTypeId,
                visitDate,
                visitDate.getDayOfWeek().name(),
                status,
                useTime,
                restDate,
                warning,
                NOTICE,
                SOURCE,
                OffsetDateTime.now(clock.withZone(SEOUL)));
    }
}
