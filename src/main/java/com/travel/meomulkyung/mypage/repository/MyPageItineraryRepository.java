package com.travel.meomulkyung.mypage.repository;

import com.travel.meomulkyung.itinerary.domain.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 마이페이지 전용 저장 일정 조회. */
public interface MyPageItineraryRepository extends JpaRepository<Itinerary, Long> {

    @Query("""
            select itinerary from Itinerary itinerary
            join fetch itinerary.region
            where itinerary.user.id = :userId and itinerary.bookmarked = true
            order by itinerary.bookmarkedAt desc
            """)
    List<Itinerary> findBookmarkedForUser(@Param("userId") Long userId);
}
