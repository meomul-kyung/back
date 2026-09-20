package com.travel.meomulkyung.region.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 지역 대표 사진 묶음. 사진 출처가 한국관광공사이므로 표기 문구를 함께 내려준다. */
public record RegionGalleryResponse(
        Long regionId,
        String regionName,
        List<Photo> photos,
        String source,
        LocalDateTime fetchedAt
) {
    public record Photo(
            Long contentId,
            String title,
            String imageUrl,
            String address
    ) {
    }
}
