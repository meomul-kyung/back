package com.travel.meomulkyung.region.service;

import com.travel.meomulkyung.recommendation.domain.CompanionType;
import com.travel.meomulkyung.recommendation.domain.PreferenceTag;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles;
import com.travel.meomulkyung.recommendation.domain.RegionRecommendationProfiles.RegionRecommendationProfile;
import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.domain.RegionTagScore;
import com.travel.meomulkyung.region.domain.Tag;
import com.travel.meomulkyung.region.domain.TagType;
import com.travel.meomulkyung.region.repository.RegionRepository;
import com.travel.meomulkyung.region.repository.RegionTagScoreRepository;
import com.travel.meomulkyung.region.repository.TagRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RegionSeedDataInitializer implements ApplicationRunner {

    private final RegionRepository regionRepository;
    private final TagRepository tagRepository;
    private final RegionTagScoreRepository regionTagScoreRepository;

    @Override
    public void run(ApplicationArguments args) {
        initialize();
    }

    @Transactional
    public void initialize() {
        Map<String, Tag> tagsByCode = seedTags();
        for (RegionRecommendationProfile profile : RegionRecommendationProfiles.ALL) {
            Region region = regionRepository.findByName(profile.regionName())
                    .orElseGet(() -> regionRepository.save(new Region(profile.regionId(), profile.regionName(), null, null,
                            profile.identityStatement(), null)));
            seedScores(region, profile, tagsByCode);
        }
    }

    private Map<String, Tag> seedTags() {
        Map<String, Tag> tagsByCode = new HashMap<>();
        for (PreferenceTag preferenceTag : PreferenceTag.values()) {
            tagsByCode.put(preferenceTag.getCode(), tagRepository.findByCode(preferenceTag.getCode())
                    .orElseGet(() -> tagRepository.save(new Tag(preferenceTag.getCode(), preferenceTag.getLabel(), TagType.TASTE))));
        }
        for (CompanionType companionType : CompanionType.values()) {
            tagsByCode.put(companionType.getCode(), tagRepository.findByCode(companionType.getCode())
                    .orElseGet(() -> tagRepository.save(new Tag(companionType.getCode(), companionType.getLabel(), TagType.COMPANION))));
        }
        return tagsByCode;
    }

    private void seedScores(Region region, RegionRecommendationProfile profile, Map<String, Tag> tagsByCode) {
        profile.tagScores().forEach((tag, score) -> saveScoreIfAbsent(region, tagsByCode.get(tag.getCode()), score));
        profile.companionScores().forEach((tag, score) -> saveScoreIfAbsent(region, tagsByCode.get(tag.getCode()), score));
    }

    private void saveScoreIfAbsent(Region region, Tag tag, int score) {
        if (regionTagScoreRepository.findByRegionAndTag(region, tag).isEmpty()) {
            regionTagScoreRepository.save(new RegionTagScore(region, tag, score));
        }
    }
}
