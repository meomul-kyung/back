package com.travel.meomulkyung.region.service;

import com.travel.meomulkyung.region.repository.RegionRepository;
import com.travel.meomulkyung.region.repository.RegionTagScoreRepository;
import com.travel.meomulkyung.region.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(RegionSeedDataInitializer.class)
class RegionSeedDataInitializerTest {

    @Autowired
    private RegionSeedDataInitializer initializer;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private RegionTagScoreRepository regionTagScoreRepository;

    @Test
    void seedsRegionsTagsAndScoresOnlyOnce() {
        initializer.initialize();
        initializer.initialize();

        assertThat(regionRepository.count()).isEqualTo(15);
        assertThat(tagRepository.count()).isEqualTo(13);
        assertThat(regionTagScoreRepository.count()).isEqualTo(15 * 13);
    }
}
