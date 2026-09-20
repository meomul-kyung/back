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
        profile.tagScores().forEach((tag, score) -> syncScore(region, tagsByCode.get(tag.getCode()), score));
        profile.companionScores().forEach((tag, score) -> syncScore(region, tagsByCode.get(tag.getCode()), score));
    }

    /**
     * 점수의 원본은 {@link RegionRecommendationProfiles}이므로 이미 저장된 값이 코드와 다르면 맞춘다.
     *
     * <p>예전에는 행이 없을 때만 넣고 기존 행은 그대로 뒀다. 그래서 코드에서 점수를 고쳐 배포해도
     * 이미 시드된 환경에서는 아무 변화가 없었고, "배포했는데 추천이 그대로"인 상태를 만들기 쉬웠다.
     *
     * <p>대신 DB에서 직접 손댄 점수는 다음 기동 때 코드 값으로 되돌아간다. 점수 조정은 DB가 아니라
     * 코드에서 하고 배포하는 것을 전제로 한다.
     */
    private void syncScore(Region region, Tag tag, int score) {
        regionTagScoreRepository.findByRegionAndTag(region, tag)
                .ifPresentOrElse(existing -> existing.changeScore(score),
                        () -> regionTagScoreRepository.save(new RegionTagScore(region, tag, score)));
    }
}
