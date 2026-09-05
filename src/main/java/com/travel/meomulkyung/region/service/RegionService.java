package com.travel.meomulkyung.region.service;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.domain.RepresentativeResource;
import com.travel.meomulkyung.region.dto.RegionResponses;
import com.travel.meomulkyung.region.exception.RegionNotFoundException;
import com.travel.meomulkyung.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    public List<RegionResponses.ListItem> getRegions() {
        return regionRepository.findAllByOrderByIdAsc().stream()
                .map(this::toListItem)
                .toList();
    }

    public RegionResponses.Detail getRegion(long regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RegionNotFoundException(regionId));
        return new RegionResponses.Detail(
                region.getId(),
                region.getName(),
                region.getHeroImageUrl(),
                region.getIdentityStatement(),
                region.getDescription(),
                region.getRepresentativeResources().stream().map(this::toResource).toList(),
                new RegionResponses.TravelStyle(
                        toOptions(region.getRepresentativeTags(), PreferenceTag::getCode, PreferenceTag::getLabel),
                        toOptions(region.getRecommendedCompanions(), CompanionType::getCode, CompanionType::getLabel)
                ),
                List.copyOf(region.getLocalTips()),
                region.getSourceAttributions().stream().map(RegionResponses.SourceAttribution::new).toList(),
                region.getUpdatedAt()
        );
    }

    private RegionResponses.ListItem toListItem(Region region) {
        return new RegionResponses.ListItem(
                region.getId(),
                region.getName(),
                region.getThumbnailUrl(),
                region.getIdentityStatement(),
                toOptions(region.getRepresentativeTags(), PreferenceTag::getCode, PreferenceTag::getLabel)
        );
    }

    private RegionResponses.RepresentativeResource toResource(RepresentativeResource resource) {
        return new RegionResponses.RepresentativeResource(
                resource.getPlaceId(), resource.getPlaceName(), resource.getCategory(),
                resource.getImageUrl(), resource.getShortDescription()
        );
    }

    private <T> List<RegionResponses.Option> toOptions(List<T> values, Function<T, String> code, Function<T, String> label) {
        return values.stream().map(value -> new RegionResponses.Option(code.apply(value), label.apply(value))).toList();
    }
}
