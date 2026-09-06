package com.travel.meomulkyung.region.repository;

import com.travel.meomulkyung.region.domain.Region;
import com.travel.meomulkyung.region.domain.RegionTagScore;
import com.travel.meomulkyung.region.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionTagScoreRepository extends JpaRepository<RegionTagScore, Long> {

    Optional<RegionTagScore> findByRegionAndTag(Region region, Tag tag);
}
