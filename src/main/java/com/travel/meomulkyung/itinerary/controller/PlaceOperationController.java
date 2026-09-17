package com.travel.meomulkyung.itinerary.controller;

import com.travel.meomulkyung.itinerary.dto.PlaceOperationResponses;
import com.travel.meomulkyung.itinerary.service.PlaceOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
public class PlaceOperationController {

    private final PlaceOperationService service;

    /** 일정 항목의 운영시간·휴무일. 장소 상세를 열 때만 호출한다. */
    @GetMapping("/{id}/items/{itemId}/operation-info")
    public PlaceOperationResponses.OperationInfo operationInfo(@AuthenticationPrincipal Long userId,
                                                               @PathVariable Long id,
                                                               @PathVariable Long itemId) {
        return service.operationInfo(userId, id, itemId);
    }
}
