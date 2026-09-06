package com.travel.meomulkyung.contribution;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 여행 결과 → 지역 기여도 산출 정책.
 *
 * <p><b>생활인구 산입 일수</b>
 * <br>「인구감소지역 지원 특별법」상 생활인구는 주민등록인구 + 체류인구 + 등록외국인으로 구성되며,
 * 체류인구는 "주민등록지가 아닌 지역을 월 1회 이상 방문하여 하루 3시간 이상 머무는 사람"으로 산정한다.
 * 이 기준을 그대로 적용해, 체류시간 중 <b>3시간 이상 머문 날</b>만 1일로 산입한다.
 *
 * <p><b>예상 소비 금액</b>
 * <br>2025년 국민여행조사(문화체육관광부·한국관광공사) 기준 국내여행 총지출 39조 5천억 원 /
 * 국내여행 총 3억 회 ≒ <b>1인 1회 평균 132,000원</b>, 숙박여행 평균 1박 2일을 적용해
 * 1인 1일 단가를 66,000원으로 둔다. 여기에 방문 인원 수와 산입 일수를 곱한다.
 *
 * <p>단가·기준은 application.properties에서 조정 가능하며, 산출값에는 항상 정책 버전을 함께 남긴다.
 */
@Component
public class ContributionPolicy {

    private final long dailySpendingPerPerson;
    private final int minimumStayHoursPerDay;
    private final String version;

    public ContributionPolicy(
            @Value("${contribution.daily-spending-per-person:66000}") long dailySpendingPerPerson,
            @Value("${contribution.minimum-stay-hours-per-day:3}") int minimumStayHoursPerDay,
            @Value("${contribution.policy-version:2025-KNTS-v1}") String version) {
        this.dailySpendingPerPerson = dailySpendingPerPerson;
        this.minimumStayHoursPerDay = minimumStayHoursPerDay;
        this.version = version;
    }

    public String version() {
        return version;
    }

    /** 체류시간·인원으로 기여 지표를 산출한다. */
    public ContributionResult calculate(int stayHours, int partySize) {
        int days = populationContributionDays(stayHours);
        int people = Math.max(partySize, 0);
        long estimatedSpending = dailySpendingPerPerson * people * days;
        return new ContributionResult(days, estimatedSpending, version);
    }

    /**
     * 생활인구 산입 일수.
     * 24시간마다 1일로 세고, 남은 시간이 최소 체류시간(기본 3시간) 이상이면 1일을 더한다.
     */
    public int populationContributionDays(int stayHours) {
        if (stayHours < minimumStayHoursPerDay) {
            return 0;
        }
        int fullDays = stayHours / 24;
        int remainderHours = stayHours % 24;
        return fullDays + (remainderHours >= minimumStayHoursPerDay ? 1 : 0);
    }
}
