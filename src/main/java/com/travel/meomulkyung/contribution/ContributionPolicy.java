package com.travel.meomulkyung.contribution;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 여행 결과 → 지역 기여도 산출 정책.
 *
 * <p><b>생활인구 산입 일수</b>
 * <br>「인구감소지역 지원 특별법」상 생활인구는 주민등록인구 + 체류인구 + 등록외국인으로 구성되며,
 * 체류인구는 "주민등록지가 아닌 지역을 월 1회 이상 방문하여 하루 3시간 이상 머무는 사람"으로 산정한다.
 * 여기서 체류 일수는 <b>날짜(캘린더 데이) 단위</b>로 세므로, 1박 2일 여행은 이틀에 걸쳐 머문 것이 되어 2일이다.
 * <br>따라서 산입 일수는 일정의 여행 일수(숙박일수 + 1)를 그대로 쓰되, 실제 체류시간이
 * 최소 기준(기본 3시간)에 못 미치면 스쳐 지나간 방문으로 보고 0일로 처리한다.
 *
 * <p><b>예상 소비 금액</b>
 * <br>2025년 국민여행조사(문화체육관광부·한국관광공사) 기준 국내여행 총지출 39조 5천억 원 /
 * 국내여행 총 3억 회 ≒ <b>1인 1회 평균 132,000원</b>이고, 숙박여행 평균이 1박 2일이므로
 * 1인 1일 단가는 66,000원이 된다. 여기에 방문 인원 수와 산입 일수를 곱한다.
 * 1박 2일 여행이면 1인당 132,000원으로, 국민여행조사의 1인 1회 평균과 정확히 일치한다.
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

    /**
     * 여행 일수·체류시간·인원으로 기여 지표를 산출한다.
     *
     * @param tripDays  여행 일수(숙박일수 + 1). 당일치기는 1
     * @param stayHours 사용자가 등록한 실제 체류시간
     * @param partySize 방문 인원 수
     */
    public ContributionResult calculate(int tripDays, int stayHours, int partySize) {
        int days = populationContributionDays(tripDays, stayHours);
        int people = Math.max(partySize, 0);
        long estimatedSpending = dailySpendingPerPerson * people * days;
        return new ContributionResult(days, estimatedSpending, version);
    }

    /**
     * 생활인구 산입 일수.
     * 체류 판정 단위가 날짜이므로 여행 일수를 그대로 세되, 최소 체류시간(기본 3시간)에
     * 못 미치는 방문은 산입하지 않는다.
     */
    public int populationContributionDays(int tripDays, int stayHours) {
        if (stayHours < minimumStayHoursPerDay) {
            return 0;
        }
        return Math.max(tripDays, 0);
    }
}
