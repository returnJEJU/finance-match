package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.match.calculator.DebtRepaymentEngine;
import com.financematch.match.calculator.FinancialAssetEngine;
import com.financematch.match.calculator.FinancialValueEngine;
import com.financematch.match.calculator.GoalFeasibilityEngine;
import com.financematch.match.calculator.TaxStrategyEngine;

import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MatchCalculationResult;
import com.financematch.match.calculator.MatchCalculator;
import com.financematch.match.calculator.MemberCalculationInput;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * 데모 시드(couple id=1, 김하나·이두리, HOUSING 3.5억/36개월) 값으로 계산기 → reason 포맷터까지
 * 전체 파이프라인을 실제로 돌려보는 실험용 테스트. DB·Spring 컨텍스트 없이 값만 그대로 옮겨 심는다.
 *
 * <p>정확한 문장을 미리 못박아 검증하기보다는(계산식이 바뀔 수 있어서), 파이프라인이 끝까지 예외 없이
 * 돌고 결과가 상식적인 범위인지만 확인한다. 실제 문장은 콘솔/테스트 리포트의 stdout에서 확인한다.
 */
class GoalFeasibilityPipelineExperimentTest {

    private final MatchCalculator calculator = new MatchCalculator(
            new FinancialAssetEngine(),
            new DebtRepaymentEngine(),
            new GoalFeasibilityEngine(),
            new FinancialValueEngine(),
            new TaxStrategyEngine()
    );
    private final GoalFeasibilityReasonFormatter formatter = new GoalFeasibilityReasonFormatter();

    @Test
    void 데모_시드_값_그대로_부족_케이스() {
        // expectedAsset(298,324,806) < targetAmount(350,000,000)
        run(demoCoupleInput(new BigDecimal("350000000")));
    }

    @Test
    void 목표금액이_훨씬_크면_부족폭도_커진다() {
        // expectedAsset(298,324,806) < targetAmount(600,000,000) — 훨씬 큰 부족
        run(demoCoupleInput(new BigDecimal("600000000")));
    }

    @Test
    void 목표금액이_예상자산보다_작으면_초과_케이스() {
        // expectedAsset(298,324,806) > targetAmount(200,000,000)
        run(demoCoupleInput(new BigDecimal("200000000")));
    }

    @Test
    void 목표금액과_예상자산이_정확히_같으면_경계값_달성_케이스() {
        // formatter 는 expectedAsset < targetAmount 일 때만 부족으로 분기하므로,
        // 같으면(<가 아니라 =) "달성" 쪽 문구가 나와야 한다 — 경계값 검증.
        MatchCalculationInput probe = demoCoupleInput(new BigDecimal("350000000"));
        BigDecimal exactExpectedAsset = calculator.calculateExpectedAsset(probe);

        run(demoCoupleInput(exactExpectedAsset));
    }

    private void run(MatchCalculationInput input) {
        MatchCalculationResult result = calculator.calculate(input);
        BigDecimal expectedAsset = calculator.calculateExpectedAsset(input);
        String reason =
                formatter.format(expectedAsset, input.getTargetAmount(), input.getTargetPeriodMonths());

        System.out.println("=== 목표 달성 가능성 파이프라인 실험 ===");
        System.out.println("goalFeasibilityScore = " + result.getGoalFeasibilityScore() + " / 20");
        System.out.println("totalScore           = " + result.getTotalScore());
        System.out.println("expectedAsset        = " + expectedAsset);
        System.out.println("targetAmount         = " + input.getTargetAmount());
        System.out.println("targetPeriodMonths   = " + input.getTargetPeriodMonths());
        System.out.println("reason               = " + reason);

        assertNotNull(expectedAsset);
        assertNotNull(reason);
        assertFalse(reason.isBlank());
        assertTrue(result.getGoalFeasibilityScore().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.getGoalFeasibilityScore().compareTo(new BigDecimal("20")) <= 0);
    }

    // db/dev-seed/R__02_seed_demo.sql 의 couple id=1 값 그대로, targetAmount 만 시나리오별로 바꾼다.
    private MatchCalculationInput demoCoupleInput(BigDecimal targetAmount) {
        BigDecimal ageGroupMedian30s = new BigDecimal("93300000"); // age_group_asset_median 30~39

        MemberCalculationInput memberA = // 김하나, 안정추구형, 32세
                MemberCalculationInput.builder()
                        .age(32)
                        .ageGroupAssetMedian(ageGroupMedian30s)
                        .financialAsset(new BigDecimal("90000000"))
                        .annualIncome(new BigDecimal("45000000"))
                        .totalDebt(new BigDecimal("20000000"))
                        .annualDebtPayment(new BigDecimal("6000000"))
                        .financialAssetRatioScore(3) // UNDER_50
                        .investmentExperienceScore(3) // MODERATE_RISK
                        .financialKnowledgeScore(3) // MEDIUM
                        .capitalPreservationScore(4) // UNDER_50
                        .monthlyAvailableAmount(new BigDecimal("1000000"))
                        .pensionSavingBalance(new BigDecimal("5000000"))
                        .irpBalance(BigDecimal.ZERO)
                        .pensionAnnualPayment(new BigDecimal("6000000"))
                        .irpAnnualPayment(BigDecimal.ZERO)
                        .dcAnnualPayment(BigDecimal.ZERO)
                        .isaAnnualDeposit(new BigDecimal("10000000"))
                        .taxEligibilityStatus("ELIGIBLE")
                        .isaEligibilityStatus("ELIGIBLE")
                        .build();

        MemberCalculationInput memberB = // 이두리, 적극투자형, 33세
                MemberCalculationInput.builder()
                        .age(33)
                        .ageGroupAssetMedian(ageGroupMedian30s)
                        .financialAsset(new BigDecimal("180000000"))
                        .annualIncome(new BigDecimal("75000000"))
                        .totalDebt(new BigDecimal("40000000"))
                        .annualDebtPayment(new BigDecimal("10000000"))
                        .financialAssetRatioScore(4) // UNDER_80
                        .investmentExperienceScore(4) // MODERATE_HIGH_RISK
                        .financialKnowledgeScore(4) // HIGH
                        .capitalPreservationScore(2) // UNDER_10
                        .monthlyAvailableAmount(new BigDecimal("1500000"))
                        .pensionSavingBalance(new BigDecimal("20000000"))
                        .irpBalance(new BigDecimal("15000000"))
                        .pensionAnnualPayment(new BigDecimal("6000000"))
                        .irpAnnualPayment(new BigDecimal("3000000"))
                        .dcAnnualPayment(BigDecimal.ZERO)
                        .isaAnnualDeposit(new BigDecimal("20000000"))
                        .taxEligibilityStatus("ELIGIBLE")
                        .isaEligibilityStatus("ELIGIBLE")
                        .build();

        return MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .firstGoalType("HOUSING") // common_survey.first_goal_type
                .targetAmount(targetAmount)
                .targetPeriodMonths(36)
                .build();
    }
}
