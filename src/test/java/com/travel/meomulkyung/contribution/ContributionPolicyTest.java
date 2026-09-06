package com.travel.meomulkyung.contribution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ContributionPolicyTest {

    private static final long DAILY_SPENDING = 66_000L;
    private static final String VERSION = "2025-KNTS-v1";

    private final ContributionPolicy policy = new ContributionPolicy(DAILY_SPENDING, 3, VERSION);

    @DisplayName("생활인구 산입 일수는 날짜 단위로 세므로 여행 일수를 그대로 따른다")
    @ParameterizedTest(name = "{0}일 여행 · 체류 {1}시간 → {2}일")
    @CsvSource({
            "1, 3, 1",      // 당일치기 최소 체류
            "1, 12, 1",
            "2, 20, 2",     // 1박 2일. 24시간이 안 돼도 날짜가 바뀌었으므로 2일
            "2, 24, 2",
            "2, 26, 2",
            "2, 40, 2",
            "3, 50, 3",     // 2박 3일
            "4, 70, 4",
            "8, 180, 8"
    })
    void populationContributionDaysFollowsTripDays(int tripDays, int stayHours, int expectedDays) {
        assertThat(policy.populationContributionDays(tripDays, stayHours)).isEqualTo(expectedDays);
    }

    @DisplayName("최소 체류시간에 못 미치는 방문은 여행 일수와 무관하게 산입하지 않는다")
    @ParameterizedTest(name = "{0}일 여행 · 체류 {1}시간 → 0일")
    @CsvSource({
            "1, 0",
            "1, 2",
            "2, 2",
            "3, 1"
    })
    void shortVisitIsNotCounted(int tripDays, int stayHours) {
        assertThat(policy.populationContributionDays(tripDays, stayHours)).isZero();
        assertThat(policy.calculate(tripDays, stayHours, 2).estimatedSpending()).isZero();
    }

    @DisplayName("예상 소비 금액은 1인 1일 단가 × 인원 × 산입 일수다")
    @ParameterizedTest(name = "{0}일 여행 · {2}명 → {3}원")
    @CsvSource({
            "2, 26, 2, 264000",   // 1박 2일 2명
            "2, 24, 1, 132000",   // 1박 2일 1명 = 국민여행조사 1인 1회 평균과 동일
            "1, 5, 1, 66000",     // 당일치기 1명
            "3, 50, 2, 396000"    // 2박 3일 2명
    })
    void estimatedSpending(int tripDays, int stayHours, int partySize, long expectedSpending) {
        assertThat(policy.calculate(tripDays, stayHours, partySize).estimatedSpending())
                .isEqualTo(expectedSpending);
    }

    @Test
    @DisplayName("1박 2일 1인 예상 소비는 국민여행조사 1인 1회 평균 132,000원과 일치한다")
    void oneNightTripMatchesNationalSurveyAverage() {
        assertThat(policy.calculate(2, 24, 1).estimatedSpending()).isEqualTo(132_000L);
    }

    @Test
    @DisplayName("산출 결과에는 항상 정책 버전이 담긴다")
    void resultCarriesPolicyVersion() {
        ContributionResult result = policy.calculate(2, 26, 2);

        assertThat(result.policyVersion()).isEqualTo(VERSION);
        assertThat(result.populationContributionDays()).isEqualTo(2);
        assertThat(policy.version()).isEqualTo(VERSION);
    }

    @Test
    @DisplayName("단가와 최소 체류시간 기준은 설정으로 바꿀 수 있다")
    void policyIsConfigurable() {
        ContributionPolicy custom = new ContributionPolicy(100_000L, 6, "custom-v1");

        // 체류 5시간은 최소 기준(6시간)에 못 미쳐 산입되지 않는다
        assertThat(custom.populationContributionDays(2, 5)).isZero();
        assertThat(custom.calculate(2, 26, 2).estimatedSpending()).isEqualTo(400_000L);
        assertThat(custom.calculate(2, 26, 2).policyVersion()).isEqualTo("custom-v1");
    }
}
