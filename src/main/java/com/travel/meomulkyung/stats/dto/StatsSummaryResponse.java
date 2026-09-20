package com.travel.meomulkyung.stats.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 서비스 전체 누적 기여 현황.
 * 개인 기록이 아니라 합계만 내려주므로 로그인 없이 조회할 수 있다.
 */
public record StatsSummaryResponse(
        long totalTrips,
        long totalTravelers,
        long totalPopulationContributionDays,
        long totalSpending,
        long totalStayHours,
        List<RegionRank> topRegions,
        LocalDateTime calculatedAt
) {
    public record RegionRank(
            int rank,
            Long regionId,
            String regionName,
            long tripCount,
            long populationContributionDays
    ) {
    }
}
