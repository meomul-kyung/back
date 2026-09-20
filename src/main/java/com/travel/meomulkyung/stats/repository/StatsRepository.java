package com.travel.meomulkyung.stats.repository;

import com.travel.meomulkyung.itinerary.domain.CompletedTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 완료 여행 전체 집계.
 * 기존 CompletedTripRepository와 MyPageCompletedTripRepository는 모두 사용자 단위 조회라
 * 전역 합계를 낼 수단이 없어 따로 둔다.
 */
public interface StatsRepository extends JpaRepository<CompletedTrip, Long> {

    interface Totals {
        long getTripCount();
        long getTravelerCount();
        Long getPopulationDays();
        Long getSpending();
        Long getStayHours();
    }

    @Query("""
            select count(t) as tripCount,
                   count(distinct t.user.id) as travelerCount,
                   coalesce(sum(t.populationContributionDays), 0) as populationDays,
                   coalesce(sum(t.estimatedSpending), 0) as spending,
                   coalesce(sum(t.stayHours), 0) as stayHours
            from CompletedTrip t
            """)
    Totals findTotals();

    interface RegionCount {
        Long getRegionId();
        String getRegionName();
        long getTripCount();
        Long getPopulationDays();
    }

    @Query("""
            select t.region.id as regionId,
                   t.region.name as regionName,
                   count(t) as tripCount,
                   coalesce(sum(t.populationContributionDays), 0) as populationDays
            from CompletedTrip t
            group by t.region.id, t.region.name
            order by count(t) desc, t.region.id asc
            """)
    List<RegionCount> findRegionRanking();
}
