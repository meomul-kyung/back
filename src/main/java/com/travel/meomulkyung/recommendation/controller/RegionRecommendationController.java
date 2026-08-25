package com.travel.meomulkyung.recommendation.controller;

import com.travel.meomulkyung.recommendation.dto.RegionRecommendationRequest;
import com.travel.meomulkyung.recommendation.dto.RegionRecommendationResponse;
import com.travel.meomulkyung.recommendation.service.RegionRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionRecommendationController {

    private final RegionRecommendationService regionRecommendationService;

    @PostMapping("/recommendations")
    @ResponseStatus(HttpStatus.OK)
    public RegionRecommendationResponse recommend(@Valid @RequestBody RegionRecommendationRequest request) {
        if (request.hasDuplicatePreferenceTags()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "preferenceTags must not contain duplicates");
        }
        return regionRecommendationService.recommend(request);
    }
}
