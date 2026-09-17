package com.travel.meomulkyung.itinerary.external;

import java.util.Optional;

/**
 * 장소 상세 정보(운영시간·휴무일) 조회.
 *
 * <p>구현체는 외부 API 실패 시 {@link TourApiProviderException}을 던진다. 호출하는 쪽에서 빈칸 처리한다.
 */
public interface PlaceDetailProvider {

    /** 공통정보(detailCommon2)로 콘텐츠 타입 코드를 조회한다. 기존 일정 항목처럼 타입을 모를 때만 쓴다. */
    Optional<String> contentTypeId(long contentId);

    /** 소개정보(detailIntro2)에서 이용시간·쉬는날 원문을 조회한다. */
    OperationHours operationHours(long contentId, String contentTypeId);

    /** 원문 그대로(태그 정리만) 담는다. 값이 없으면 null. */
    record OperationHours(String useTime, String restDate) { }
}
