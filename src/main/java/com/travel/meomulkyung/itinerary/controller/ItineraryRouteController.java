package com.travel.meomulkyung.itinerary.controller;

import com.travel.meomulkyung.itinerary.dto.ItineraryRouteResponses;
import com.travel.meomulkyung.itinerary.service.ItineraryRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
public class ItineraryRouteController {

    private final ItineraryRouteService service;

    /**
     * 일정 하루치 장소 간 이동정보. 날짜 탭을 열 때만 호출한다.
     *
     * @param mode CAR(기본) | TRANSIT
     */
    @GetMapping("/{id}/days/{dayNumber}/routes")
    public ItineraryRouteResponses.DayRoutes routes(@AuthenticationPrincipal Long userId,
                                                    @PathVariable Long id,
                                                    @PathVariable int dayNumber,
                                                    @RequestParam(required = false) String mode) {
        return service.routes(userId, id, dayNumber, mode);
    }
}
