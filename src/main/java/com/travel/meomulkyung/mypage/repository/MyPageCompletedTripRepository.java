package com.travel.meomulkyung.mypage.repository;

import com.travel.meomulkyung.itinerary.domain.CompletedTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 마이페이지 전용 완료 여행 조회.
 *
 * <p>일정 도메인의 CompletedTripRepository와 같은 엔티티를 보지만, 마이페이지 조회 쿼리를 별도로 두어
 * 일정 기능 쪽 파일을 건드리지 않는다.
 */
public interface MyPageCompletedTripRepository extends JpaRepository<CompletedTrip, Long> {

    @Query("""
            select trip from CompletedTrip trip
            join fetch trip.region
            join fetch trip.itinerary
            where trip.user.id = :userId
            order by trip.completedAt desc
            """)
    List<CompletedTrip> findAllForUser(@Param("userId") Long userId);

    @Query("""
            select trip from CompletedTrip trip
            join fetch trip.region
            join fetch trip.itinerary
            where trip.user.id = :userId and trip.region.id = :regionId
            order by trip.completedAt desc
            """)
    List<CompletedTrip> findAllForUserAndRegion(@Param("userId") Long userId, @Param("regionId") Long regionId);

    @Query("""
            select trip from CompletedTrip trip
            join fetch trip.region
            join fetch trip.itinerary
            join fetch trip.user
            where trip.id = :completedTripId
            """)
    Optional<CompletedTrip> findDetailById(@Param("completedTripId") Long completedTripId);
}
