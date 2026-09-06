package com.travel.meomulkyung.recommendation.controller;

import com.travel.meomulkyung.recommendation.dto.TravelOptionsResponse;
import com.travel.meomulkyung.recommendation.service.TravelOptionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/travel-options")
@RequiredArgsConstructor
public class TravelOptionsController {

    private final TravelOptionsService travelOptionsService;

    @GetMapping
    public TravelOptionsResponse getTravelOptions() {
        return travelOptionsService.getTravelOptions();
    }
}
