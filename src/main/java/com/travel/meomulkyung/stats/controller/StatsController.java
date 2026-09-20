package com.travel.meomulkyung.stats.controller;

import com.travel.meomulkyung.stats.dto.StatsSummaryResponse;
import com.travel.meomulkyung.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/summary")
    public StatsSummaryResponse getSummary() {
        return statsService.getSummary();
    }
}
