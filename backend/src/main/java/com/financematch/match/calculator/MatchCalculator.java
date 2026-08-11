package com.financematch.match.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class MatchCalculator {

    /*
     * 기존 report 코드가 MatchCalculator의 상수를 사용하고 있다면
     * 컴파일 오류가 발생하지 않도록 호환용 상수를 유지한다.
     */
    public static final BigDecimal PENSION_SAVING_ANNUAL_LIMIT =
            TaxStrategyEngine.PENSION_SAVING_ANNUAL_LIMIT;

    public static final BigDecimal PENSION_IRP_ANNUAL_LIMIT =
            TaxStrategyEngine.PENSION_IRP_ANNUAL_LIMIT;

    public static final BigDecimal ISA_ANNUAL_LIMIT_AMOUNT =
            TaxStrategyEngine.ISA_ANNUAL_LIMIT_AMOUNT;

    private final FinancialAssetEngine financialAssetEngine;
    private final DebtRepaymentEngine debtRepaymentEngine;
    private final GoalFeasibilityEngine goalFeasibilityEngine;
    private final FinancialValueEngine financialValueEngine;
    private final TaxStrategyEngine taxStrategyEngine;

    public MatchCalculator(
            FinancialAssetEngine financialAssetEngine,
            DebtRepaymentEngine debtRepaymentEngine,
            GoalFeasibilityEngine goalFeasibilityEngine,
            FinancialValueEngine financialValueEngine,
            TaxStrategyEngine taxStrategyEngine
    ) {
        this.financialAssetEngine = financialAssetEngine;
        this.debtRepaymentEngine = debtRepaymentEngine;
        this.goalFeasibilityEngine = goalFeasibilityEngine;
        this.financialValueEngine = financialValueEngine;
        this.taxStrategyEngine = taxStrategyEngine;
    }

    /**
     * 다섯 개 엔진의 점수를 계산하고 최종 결과를 생성한다.
     */
    public MatchCalculationResult calculate(
            MatchCalculationInput input
    ) {
        validateInput(input);

        BigDecimal assetStabilityScore =
                financialAssetEngine.calculateScore(input);

        double coupleAssetRatio =
                financialAssetEngine
                        .calculateCoupleAssetRatio(input);

        BigDecimal debtRepaymentScore =
                debtRepaymentEngine.calculateScore(input);

        BigDecimal memberADebtScore = round(
                debtRepaymentEngine.calculateMemberScore(
                        input.getMemberA()
                )
        );

        BigDecimal memberBDebtScore = round(
                debtRepaymentEngine.calculateMemberScore(
                        input.getMemberB()
                )
        );

        BigDecimal financialValueScore =
                financialValueEngine.calculateScore(input);

        BigDecimal goalFeasibilityScore =
                goalFeasibilityEngine.calculateScore(input);

        BigDecimal expectedAsset =
                goalFeasibilityEngine
                        .calculateExpectedAsset(input);

        BigDecimal taxStrategyScore =
                taxStrategyEngine.calculateScore(input);

        boolean taxStrategyCalculated =
                taxStrategyEngine.isCalculated(input);

        BigDecimal scoreSum =
                assetStabilityScore
                        .setScale(0, RoundingMode.HALF_UP)
                        .add(
                                debtRepaymentScore.setScale(
                                        0,
                                        RoundingMode.HALF_UP
                                )
                        )
                        .add(
                                financialValueScore.setScale(
                                        0,
                                        RoundingMode.HALF_UP
                                )
                        )
                        .add(
                                goalFeasibilityScore.setScale(
                                        0,
                                        RoundingMode.HALF_UP
                                )
                        )
                        .add(
                                taxStrategyScore.setScale(
                                        0,
                                        RoundingMode.HALF_UP
                                )
                        );

        BigDecimal calculatedMaxScore =
                taxStrategyCalculated
                        ? new BigDecimal("100")
                        : new BigDecimal("90");

        BigDecimal totalScore =
                scoreSum
                        .multiply(new BigDecimal("100"))
                        .divide(
                                calculatedMaxScore,
                                0,
                                RoundingMode.HALF_UP
                        );

        return MatchCalculationResult.builder()
                .assetStabilityScore(assetStabilityScore)
                .coupleAssetRatio(coupleAssetRatio)
                .debtRepaymentScore(debtRepaymentScore)
                .memberADebtScore(memberADebtScore)
                .memberBDebtScore(memberBDebtScore)
                .financialValueScore(financialValueScore)
                .goalFeasibilityScore(goalFeasibilityScore)
                .expectedAsset(expectedAsset)
                .taxStrategyScore(taxStrategyScore)
                .taxStrategyCalculated(taxStrategyCalculated)
                .totalScore(totalScore)
                .build();
    }

    /*
     * 아래 메서드는 기존 report 코드와의 호환을 위해 유지한다.
     * 실제 계산은 각 엔진에 위임한다.
     */

    public BigDecimal calculateAssetStabilityScore(
            MatchCalculationInput input
    ) {
        return financialAssetEngine.calculateScore(input);
    }

    public double calculateCoupleAssetRatio(
            MatchCalculationInput input
    ) {
        return financialAssetEngine
                .calculateCoupleAssetRatio(input);
    }

    public BigDecimal calculateDebtRepaymentScore(
            MatchCalculationInput input
    ) {
        return debtRepaymentEngine.calculateScore(input);
    }

    public double calculateMemberDebtScore(
            MemberCalculationInput member
    ) {
        return debtRepaymentEngine.calculateMemberScore(member);
    }

    public BigDecimal calculateGoalFeasibilityScore(
            MatchCalculationInput input
    ) {
        return goalFeasibilityEngine.calculateScore(input);
    }

    public BigDecimal calculateExpectedAsset(
            MatchCalculationInput input
    ) {
        return goalFeasibilityEngine
                .calculateExpectedAsset(input);
    }

    public BigDecimal calculateFinancialValueScore(
            MatchCalculationInput input
    ) {
        return financialValueEngine.calculateScore(input);
    }

    public BigDecimal calculateTaxStrategyScore(
            MatchCalculationInput input
    ) {
        return taxStrategyEngine.calculateScore(input);
    }

    private void validateInput(MatchCalculationInput input) {
        if (input == null
                || input.getMemberA() == null
                || input.getMemberB() == null) {
            throw new IllegalArgumentException(
                    "두 회원의 궁합 계산 입력값이 필요합니다."
            );
        }
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}