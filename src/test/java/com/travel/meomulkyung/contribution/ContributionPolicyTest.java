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

    @DisplayName("생활인구 산입 일수는 24시간마다 1일, 남은 시간이 3시간 이상이면 1일을 더한다")
    @ParameterizedTest(name = "체류 {0}시간 → {1}일")
    @CsvSource({
            "0, 0",     // 체류 없음
            "2, 0",     // 최소 체류시간 미만
            "3, 1",     // 최소 체류시간 경계
            "23, 1",
            "24, 1",    // 정확히 하루
            "26, 1",    // 나머지 2시간은 산입되지 않음
            "27, 2",    // 나머지 3시간은 산입됨
            "48, 2",
            "50, 2",
            "51, 3",
            "192, 8"    // 완료 등록 최대 체류시간
    })
    void populationContributionDays(int stayHours, int expectedDays) {
        assertThat(policy.populationContributionDays(stayHours)).isEqualTo(expectedDays);
    }

    @DisplayName("예상 소비 금액은 1인 1일 단가 × 인원 × 산입 일수다")
    @ParameterizedTest(name = "체류 {0}시간 · {1}명 → {2}원")
    @CsvSource({
            "50, 2, 264000",
            "24, 1, 66000",
            "27, 3, 396000",
            "2, 5, 0"       // 산입 일수가 0이면 소비도 0
    })
    void estimatedSpending(int stayHours, int partySize, long expectedSpending) {
        assertThat(policy.calculate(stayHours, partySize).estimatedSpending()).isEqualTo(expectedSpending);
    }

    @Test
    @DisplayName("산출 결과에는 항상 정책 버전이 담긴다")
    void resultCarriesPolicyVersion() {
        ContributionResult result = policy.calculate(50, 2);

        assertThat(result.policyVersion()).isEqualTo(VERSION);
        assertThat(result.populationContributionDays()).isEqualTo(2);
        assertThat(policy.version()).isEqualTo(VERSION);
    }

    @Test
    @DisplayName("단가와 최소 체류시간 기준은 설정으로 바꿀 수 있다")
    void policyIsConfigurable() {
        ContributionPolicy custom = new ContributionPolicy(100_000L, 6, "custom-v1");

        // 나머지 3시간은 최소 기준(6시간)에 못 미쳐 산입되지 않는다
        assertThat(custom.populationContributionDays(27)).isEqualTo(1);
        assertThat(custom.calculate(27, 2).estimatedSpending()).isEqualTo(200_000L);
        assertThat(custom.calculate(27, 2).policyVersion()).isEqualTo("custom-v1");
    }
}
