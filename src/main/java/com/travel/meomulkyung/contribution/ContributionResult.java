package com.travel.meomulkyung.contribution;

/**
 * 기여도 산출 결과.
 *
 * @param populationContributionDays 생활인구 산입 일수
 * @param estimatedSpending          국민여행조사 단가 기반 예상 소비 금액(원)
 * @param policyVersion              산출에 사용한 정책 버전
 */
public record ContributionResult(
        int populationContributionDays,
        long estimatedSpending,
        String policyVersion) {
}
