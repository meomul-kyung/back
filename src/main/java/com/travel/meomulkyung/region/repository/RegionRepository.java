package com.travel.meomulkyung.region.repository;

import com.travel.meomulkyung.region.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByName(String name);

    @Query("select distinct region from Region region left join fetch region.regionTagScores score left join fetch score.tag")
    List<Region> findAllWithTagScores();
}
