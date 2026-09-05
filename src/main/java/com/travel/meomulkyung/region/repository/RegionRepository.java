package com.travel.meomulkyung.region.repository;

import com.travel.meomulkyung.region.domain.Region;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    @EntityGraph(attributePaths = "representativeTags")
    List<Region> findAllByOrderByIdAsc();
}
