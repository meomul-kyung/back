package com.travel.meomulkyung.region.service;

import com.travel.meomulkyung.itinerary.external.TourPlaceProvider;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.dto.RegionGalleryResponse;
import com.travel.meomulkyung.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RegionGalleryService {

    /** 화면에서 쓰는 장 수. 더 받아도 쓰지 않으므로 여기서 끊는다. */
    private static final int PHOTO_LIMIT = 5;

    /**
     * TourAPI 개발계정은 기능별 하루 1,000건이다. 지역 상세를 열 때마다 호출하면 금세 소진되고,
     * 대표 사진은 자주 바뀌지 않으므로 지역별로 일정 시간 재사용한다.
     */
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final RegionRepository regionRepository;
    private final TourPlaceProvider placeProvider;

    private record Cached(RegionGalleryResponse response, LocalDateTime storedAt) {
    }

    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public RegionGalleryResponse getGallery(Long regionId) {
        Cached cached = cache.get(regionId);
        if (cached != null && cached.storedAt().isAfter(LocalDateTime.now().minus(CACHE_TTL))) {
            return cached.response();
        }

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RegionGalleryException(
                        HttpStatus.NOT_FOUND, "REGION_NOT_FOUND", "지역을 찾을 수 없습니다."));

        List<RegionGalleryResponse.Photo> photos = placeProvider.findPlaces(region).stream()
                .filter(place -> place.imageUrl() != null && !place.imageUrl().isBlank())
                .map(place -> new RegionGalleryResponse.Photo(
                        place.contentId(), place.title(), place.imageUrl(), place.address()))
                .limit(PHOTO_LIMIT)
                .toList();

        RegionGalleryResponse response = new RegionGalleryResponse(
                region.getId(), region.getName(), photos, "한국관광공사", LocalDateTime.now());
        cache.put(regionId, new Cached(response, LocalDateTime.now()));
        return response;
    }
}
