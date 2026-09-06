package com.travel.meomulkyung.itinerary.repository;
import com.travel.meomulkyung.itinerary.domain.Itinerary; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface ItineraryRepository extends JpaRepository<Itinerary,Long>{@Query("select distinct i from Itinerary i join fetch i.user join fetch i.region left join fetch i.items where i.id=:id") Optional<Itinerary> findDetailById(Long id);}
