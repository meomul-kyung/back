package com.travel.meomulkyung.mypage.controller;

import com.travel.meomulkyung.mypage.dto.MyPageResponses;
import com.travel.meomulkyung.mypage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 조회 엔드포인트.
 * 모든 엔드포인트는 Authorization: Bearer {JWT} 인증이 필요하며 본인 데이터만 조회한다.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    /** 완료 여행 목록 조회. regionId를 전달하면 특정 지역의 완료 여행만 조회한다. */
    @GetMapping("/completed-trips")
    public MyPageResponses.CompletedTrips completedTrips(@AuthenticationPrincipal Long userId,
                                                         @RequestParam(required = false) Long regionId) {
        return myPageService.completedTrips(userId, regionId);
    }

    /** 완료 여행·기여도 상세 조회. */
    @GetMapping("/completed-trips/{completedTripId}")
    public MyPageResponses.CompletedTripDetail completedTrip(@AuthenticationPrincipal Long userId,
                                                             @PathVariable Long completedTripId) {
        return myPageService.completedTrip(userId, completedTripId);
    }

    /** 15개 지역별 스탬프·방문 횟수 조회. */
    @GetMapping("/stamps")
    public MyPageResponses.Stamps stamps(@AuthenticationPrincipal Long userId) {
        return myPageService.stamps(userId);
    }

    /** 방문 지역·체류시간·지출·생활인구 산입 결과 요약 조회. */
    @GetMapping("/contribution-summary")
    public MyPageResponses.ContributionSummary contributionSummary(@AuthenticationPrincipal Long userId) {
        return myPageService.contributionSummary(userId);
    }

    /** 저장(책갈피)한 일정 목록 조회. */
    @GetMapping("/bookmarked-itineraries")
    public MyPageResponses.BookmarkedItineraries bookmarkedItineraries(@AuthenticationPrincipal Long userId) {
        return myPageService.bookmarkedItineraries(userId);
    }
}
