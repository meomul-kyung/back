package com.travel.meomulkyung.itinerary.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class PlaceOperationResponses {

    private PlaceOperationResponses() {
    }

    /**
     * @param status           OK | NO_DATA | NOT_SUPPORTED | UNAVAILABLE
     * @param visitDayOfWeek   MONDAY ~ SUNDAY
     * @param useTime          이용시간 원문 (줄바꿈 포함, 없으면 null)
     * @param restDate         쉬는날 원문 (줄바꿈 포함, 없으면 null)
     * @param closedDayWarning 방문일이 휴무일일 수 있으면 값이 있고, 아니면 null
     * @param notice           화면에 반드시 함께 보여줄 안내 문구
     * @param source           출처 표기 (ⓒ한국관광공사)
     * @param fetchedAt        조회 시각. 결과는 저장하지 않는다.
     */
    public record OperationInfo(Long itemId, Long placeId, String contentTypeId, LocalDate visitDate,
                                String visitDayOfWeek, String status, String useTime, String restDate,
                                ClosedDayWarning closedDayWarning, String notice, String source,
                                OffsetDateTime fetchedAt) {
    }

    /** @param level 현재는 POSSIBLE_CLOSED만 사용한다. (단정하지 않음) */
    public record ClosedDayWarning(String level, String message) {
    }
}
