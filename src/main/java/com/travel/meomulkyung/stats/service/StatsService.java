package com.travel.meomulkyung.stats.service;

import com.travel.meomulkyung.stats.dto.StatsSummaryResponse;
import com.travel.meomulkyung.stats.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class StatsService {

    /** 상위 몇 개 지역까지 내려줄지. 화면에서 쓰는 만큼만 보낸다. */
    private static final int TOP_REGION_LIMIT = 5;

    private final StatsRepository statsRepository;

    @Transactional(readOnly = true)
    public StatsSummaryResponse getSummary() {
        StatsRepository.Totals totals = statsRepository.findTotals();
        List<StatsRepository.RegionCount> ranking = statsRepository.findRegionRanking();

        List<StatsSummaryResponse.RegionRank> topRegions = IntStream.range(0, Math.min(ranking.size(), TOP_REGION_LIMIT))
                .mapToObj(index -> {
                    StatsRepository.RegionCount row = ranking.get(index);
                    return new StatsSummaryResponse.RegionRank(
                            index + 1,
                            row.getRegionId(),
                            row.getRegionName(),
                            row.getTripCount(),
                            orZero(row.getPopulationDays())
                    );
                })
                .toList();

        return new StatsSummaryResponse(
                totals.getTripCount(),
                totals.getTravelerCount(),
                orZero(totals.getPopulationDays()),
                orZero(totals.getSpending()),
                orZero(totals.getStayHours()),
                topRegions,
                LocalDateTime.now()
        );
    }

    /** 완료된 여행이 하나도 없으면 sum이 null로 올 수 있다. */
    private long orZero(Long value) {
        return value == null ? 0L : value;
    }
}
