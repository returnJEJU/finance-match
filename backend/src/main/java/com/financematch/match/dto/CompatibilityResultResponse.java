package com.financematch.match.dto;

import java.math.BigDecimal;

import com.financematch.match.domain.CompatibilityResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompatibilityResultResponse {

    private Long coupleId;

    private BigDecimal assetStabilityScore;
    private BigDecimal debtRepaymentScore;
    private BigDecimal financialValueScore;
    private BigDecimal goalFeasibilityScore;
    private BigDecimal taxStrategyScore;

    private boolean taxStrategyCalculated;

    private BigDecimal totalScore;
    private String resultSummary;

    public static CompatibilityResultResponse from(
            CompatibilityResult result
    ) {
        if (result == null) {
            throw new IllegalArgumentException(
                    "금융 궁합도 결과가 필요합니다."
            );
        }

        return CompatibilityResultResponse.builder()
                .coupleId(result.getCoupleId())
                .assetStabilityScore(
                        result.getAssetStabilityScore()
                )
                .debtRepaymentScore(
                        result.getDebtRepaymentScore()
                )
                .financialValueScore(
                        result.getFinancialValueScore()
                )
                .goalFeasibilityScore(
                        result.getGoalFeasibilityScore()
                )
                .taxStrategyScore(
                        result.getTaxStrategyScore()
                )
                .taxStrategyCalculated(
                        result.isTaxStrategyCalculated()
                )
                .totalScore(result.getTotalScore())
                .resultSummary(result.getResultSummary())
                .build();
    }
}