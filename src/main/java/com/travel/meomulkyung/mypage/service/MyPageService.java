package com.travel.meomulkyung.mypage.service;

import com.travel.meomulkyung.contribution.ContributionPolicy;
import com.travel.meomulkyung.contribution.ContributionResult;
import com.travel.meomulkyung.itinerary.domain.CompletedTrip;
import com.travel.meomulkyung.itinerary.domain.Itinerary;
import com.travel.meomulkyung.itinerary.domain.UserStamp;
import com.travel.meomulkyung.mypage.dto.MyPageResponses;
import com.travel.meomulkyung.mypage.repository.MyPageCompletedTripRepository;
import com.travel.meomulkyung.mypage.repository.MyPageItineraryRepository;
import com.travel.meomulkyung.mypage.repository.MyPageUserStampRepository;
import com.travel.meomulkyung.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 마이페이지 조회 서비스.
 *
 * <p>모든 조회는 인증 사용자 본인 데이터로 한정한다. 기여 지표는 여행 완료 시점에 저장된 스냅샷을 그대로 읽되,
 * 정책 버전이 비어 있는(기여도 도입 이전에 저장된) 기록은 조회 시점에 현재 정책으로 계산해 채워준다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MyPageCompletedTripRepository completedTrips;
    private final MyPageUserStampRepository userStamps;
    private final MyPageItineraryRepository itineraries;
    private final RegionRepository regions;
    private final ContributionPolicy contributionPolicy;

    /** 완료 여행 목록. regionId를 주면 해당 지역 여행만 조회한다. */
    public MyPageResponses.CompletedTrips completedTrips(Long userId, Long regionId) {
        List<CompletedTrip> found = (regionId == null)
                ? completedTrips.findAllForUser(userId)
                : completedTrips.findAllForUserAndRegion(userId, regionId);

        List<MyPageResponses.CompletedTripSummary> summaries = found.stream()
                .map(this::toSummary)
                .toList();
        return new MyPageResponses.CompletedTrips(summaries.size(), summaries);
    }

    /** 완료 여행 한 건의 기여도 상세. 여행 완료 직후 화면과 마이페이지 재조회가 함께 쓴다. */
    public MyPageResponses.CompletedTripDetail completedTrip(Long userId, Long completedTripId) {
        CompletedTrip trip = completedTrips.findDetailById(completedTripId)
                .orElseThrow(() -> new MyPageException(HttpStatus.NOT_FOUND,
                        "COMPLETED_TRIP_NOT_FOUND", "완료된 여행을 찾을 수 없습니다."));
        if (!trip.getUser().getId().equals(userId)) {
            throw new MyPageException(HttpStatus.FORBIDDEN,
                    "COMPLETED_TRIP_ACCESS_DENIED", "여행 기록 접근 권한이 없습니다.");
        }

        ContributionResult contribution = contributionOf(trip);
        Itinerary itinerary = trip.getItinerary();
        MyPageResponses.StampBadge stamp = userStamps.findAllForUser(userId).stream()
                .filter(found -> found.getRegion().getId().equals(trip.getRegion().getId()))
                .findFirst()
                .map(found -> collected(found))
                .orElseGet(() -> notCollected(trip.getRegion().getId(), trip.getRegion().getName()));

        return new MyPageResponses.CompletedTripDetail(
                trip.getId(),
                itinerary.getId(),
                region(trip.getRegion().getId(), trip.getRegion().getName()),
                itinerary.getTitle(),
                itinerary.getStartDate(),
                itinerary.getEndDate(),
                itinerary.getNights(),
                trip.getCompletedAt(),
                new MyPageResponses.Contribution(
                        trip.getStayHours(),
                        trip.getPartySize(),
                        trip.getTotalSpent(),
                        contribution.estimatedSpending(),
                        contribution.populationContributionDays(),
                        contribution.policyVersion()),
                stamp);
    }

    /** 15개 지역 스탬프 현황. 미수집 지역도 함께 내려 프론트가 배지 그리드를 그대로 그릴 수 있게 한다. */
    public MyPageResponses.Stamps stamps(Long userId) {
        Map<Long, UserStamp> collected = new LinkedHashMap<>();
        userStamps.findAllForUser(userId)
                .forEach(stamp -> collected.put(stamp.getRegion().getId(), stamp));

        List<MyPageResponses.StampBadge> badges = regions.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(region -> {
                    UserStamp stamp = collected.get(region.getId());
                    return stamp == null
                            ? notCollected(region.getId(), region.getName())
                            : collected(stamp);
                })
                .toList();

        int totalVisitCount = collected.values().stream().mapToInt(UserStamp::getVisitCount).sum();
        return new MyPageResponses.Stamps(collected.size(), badges.size(), totalVisitCount, badges);
    }

    /** 지역 기여도 요약. 전체 누계와 지역별 누계를 함께 준다. */
    public MyPageResponses.ContributionSummary contributionSummary(Long userId) {
        List<CompletedTrip> trips = completedTrips.findAllForUser(userId);

        Map<Long, MyPageResponses.RegionContribution> byRegion = new LinkedHashMap<>();
        int totalStayHours = 0;
        long totalReportedSpending = 0;
        long totalEstimatedSpending = 0;
        int totalPopulationContributionDays = 0;

        for (CompletedTrip trip : trips) {
            ContributionResult contribution = contributionOf(trip);
            totalStayHours += trip.getStayHours();
            totalReportedSpending += trip.getTotalSpent();
            totalEstimatedSpending += contribution.estimatedSpending();
            totalPopulationContributionDays += contribution.populationContributionDays();

            Long regionId = trip.getRegion().getId();
            MyPageResponses.RegionContribution previous = byRegion.get(regionId);
            byRegion.put(regionId, previous == null
                    ? new MyPageResponses.RegionContribution(
                    regionId,
                    trip.getRegion().getName(),
                    1,
                    trip.getStayHours(),
                    contribution.estimatedSpending(),
                    contribution.populationContributionDays())
                    : new MyPageResponses.RegionContribution(
                    regionId,
                    previous.regionName(),
                    previous.tripCount() + 1,
                    previous.stayHours() + trip.getStayHours(),
                    previous.estimatedSpending() + contribution.estimatedSpending(),
                    previous.populationContributionDays() + contribution.populationContributionDays()));
        }

        return new MyPageResponses.ContributionSummary(
                byRegion.size(),
                trips.size(),
                totalStayHours,
                totalReportedSpending,
                totalEstimatedSpending,
                totalPopulationContributionDays,
                contributionPolicy.version(),
                new ArrayList<>(byRegion.values()));
    }

    /** 저장(책갈피)한 일정 목록. */
    public MyPageResponses.BookmarkedItineraries bookmarkedItineraries(Long userId) {
        List<MyPageResponses.BookmarkedItinerary> found = itineraries.findBookmarkedForUser(userId).stream()
                .map(itinerary -> new MyPageResponses.BookmarkedItinerary(
                        itinerary.getId(),
                        region(itinerary.getRegion().getId(), itinerary.getRegion().getName()),
                        itinerary.getTitle(),
                        itinerary.getStartDate(),
                        itinerary.getEndDate(),
                        itinerary.getNights(),
                        itinerary.getStatus().name(),
                        itinerary.getBookmarkedAt()))
                .toList();
        return new MyPageResponses.BookmarkedItineraries(found.size(), found);
    }

    /**
     * 완료 시점에 저장된 기여도 스냅샷을 읽는다.
     * 정책 버전이 없으면(기여도 산출 도입 이전 기록) 현재 정책으로 계산해 응답에만 채운다.
     */
    private ContributionResult contributionOf(CompletedTrip trip) {
        if (trip.getContributionPolicyVersion() != null) {
            return new ContributionResult(
                    trip.getPopulationContributionDays(),
                    trip.getEstimatedSpending(),
                    trip.getContributionPolicyVersion());
        }
        return contributionPolicy.calculate(trip.getStayHours(), trip.getPartySize());
    }

    private MyPageResponses.CompletedTripSummary toSummary(CompletedTrip trip) {
        ContributionResult contribution = contributionOf(trip);
        Itinerary itinerary = trip.getItinerary();
        return new MyPageResponses.CompletedTripSummary(
                trip.getId(),
                itinerary.getId(),
                region(trip.getRegion().getId(), trip.getRegion().getName()),
                itinerary.getTitle(),
                itinerary.getStartDate(),
                itinerary.getEndDate(),
                itinerary.getNights(),
                trip.getStayHours(),
                trip.getPartySize(),
                contribution.estimatedSpending(),
                contribution.populationContributionDays(),
                trip.getCompletedAt());
    }

    private MyPageResponses.Region region(Long regionId, String regionName) {
        return new MyPageResponses.Region(regionId, regionName);
    }

    private MyPageResponses.StampBadge collected(UserStamp stamp) {
        return new MyPageResponses.StampBadge(
                stamp.getRegion().getId(),
                stamp.getRegion().getName(),
                true,
                stamp.getVisitCount(),
                stamp.getFirstAwardedAt(),
                stamp.getLastVisitedAt());
    }

    private MyPageResponses.StampBadge notCollected(Long regionId, String regionName) {
        return new MyPageResponses.StampBadge(regionId, regionName, false, 0, null, null);
    }
}
