package com.travel.meomulkyung.region.controller;

import com.travel.meomulkyung.region.dto.RegionGalleryResponse;
import com.travel.meomulkyung.region.service.RegionGalleryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionGalleryController {

    private final RegionGalleryService regionGalleryService;

    @GetMapping("/{regionId}/gallery")
    public RegionGalleryResponse getGallery(@PathVariable Long regionId) {
        return regionGalleryService.getGallery(regionId);
    }
}
