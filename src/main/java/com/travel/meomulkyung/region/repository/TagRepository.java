package com.travel.meomulkyung.region.repository;

import com.travel.meomulkyung.region.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByCode(String code);
}
