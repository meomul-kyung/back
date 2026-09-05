package com.travel.meomulkyung.region.controller;

import com.travel.meomulkyung.region.dto.RegionResponses;
import com.travel.meomulkyung.region.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    public List<RegionResponses.ListItem> getRegions() {
        return regionService.getRegions();
    }

    @GetMapping("/{regionId}")
    public RegionResponses.Detail getRegion(@PathVariable long regionId) {
        return regionService.getRegion(regionId);
    }
}
