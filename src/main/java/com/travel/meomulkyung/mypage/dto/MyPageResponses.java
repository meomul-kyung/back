package com.travel.meomulkyung.mypage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 마이페이지 조회 응답 모음. */
public final class MyPageResponses {

    private MyPageResponses() {
    }

    public record Region(Long regionId, String regionName) {
    }

    /** 완료 여행 목록의 한 건. */
    public record CompletedTripSummary(
            Long completedTripId,
            Long itineraryId,
            Region region,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            int nights,
            int stayHours,
            int partySize,
            long estimatedSpending,
            int populationContributionDays,
            LocalDateTime completedAt) {
    }

    public record CompletedTrips(int totalCount, List<CompletedTripSummary> completedTrips) {
    }

    /**
     * 기여 지표.
     *
     * @param reportedSpending           여행 완료 등록 시 사용자가 입력한 실제 지출(원)
     * @param estimatedSpending          국민여행조사 단가 기반 예상 소비 금액(원)
     * @param populationContributionDays 생활인구 산입 일수
     * @param policyVersion              산출에 사용한 기여도 정책 버전
     */
    public record Contribution(
            int stayHours,
            int partySize,
            long reportedSpending,
            long estimatedSpending,
            int populationContributionDays,
            String policyVersion) {
    }

    /** 지역 배지 한 칸. 미수집 지역도 collected=false로 함께 내려준다. */
    public record StampBadge(
            Long regionId,
            String regionName,
            boolean collected,
            int visitCount,
            LocalDateTime firstAwardedAt,
            LocalDateTime lastVisitedAt) {
    }

    public record CompletedTripDetail(
            Long completedTripId,
            Long itineraryId,
            Region region,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            int nights,
            LocalDateTime completedAt,
            Contribution contribution,
            StampBadge stamp) {
    }

    public record Stamps(
            int collectedCount,
            int totalRegionCount,
            int totalVisitCount,
            List<StampBadge> stamps) {
    }

    /** 지역별 기여 누계. */
    public record RegionContribution(
            Long regionId,
            String regionName,
            int tripCount,
            int stayHours,
            long estimatedSpending,
            int populationContributionDays) {
    }

    public record ContributionSummary(
            int visitedRegionCount,
            int completedTripCount,
            int totalStayHours,
            long totalReportedSpending,
            long totalEstimatedSpending,
            int totalPopulationContributionDays,
            String policyVersion,
            List<RegionContribution> regions) {
    }

    public record BookmarkedItinerary(
            Long itineraryId,
            Region region,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            int nights,
            String status,
            LocalDateTime bookmarkedAt) {
    }

    public record BookmarkedItineraries(int totalCount, List<BookmarkedItinerary> itineraries) {
    }
}
