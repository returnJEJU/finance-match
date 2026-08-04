package com.financematch.match.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MatchCalculatorTest {

    private final MatchCalculator calculator = new MatchCalculator();

    @Test
    void 금융자산_점수를_계산한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .age(32)
                .ageGroupAssetMedian(new BigDecimal("93300000"))
                .financialAsset(new BigDecimal("90000000"))
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .age(33)
                .ageGroupAssetMedian(new BigDecimal("93300000"))
                .financialAsset(new BigDecimal("180000000"))
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        BigDecimal result = calculator.calculateAssetStabilityScore(input);

        System.out.println("금융자산 점수: " + result);
        assertEquals(new BigDecimal("18.62"), result);
    }

    @Test
    void 부채_점수를_계산한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .annualIncome(new BigDecimal("45000000"))
                .totalDebt(new BigDecimal("20000000"))
                .annualDebtPayment(new BigDecimal("6000000"))
                .financialAsset(new BigDecimal("90000000"))
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .annualIncome(new BigDecimal("75000000"))
                .totalDebt(new BigDecimal("40000000"))
                .annualDebtPayment(new BigDecimal("10000000"))
                .financialAsset(new BigDecimal("180000000"))
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        BigDecimal result = calculator.calculateDebtRepaymentScore(input);

        System.out.println("부채 점수: " + result);
        assertEquals(new BigDecimal("14.00"), result);
    }

    @Test
    void 투자_가치관_일치도_점수를_계산한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .financialAssetRatioScore(3)
                .investmentExperienceScore(3)
                .financialKnowledgeScore(3)
                .capitalPreservationScore(4)
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .financialAssetRatioScore(4)
                .investmentExperienceScore(4)
                .financialKnowledgeScore(4)
                .capitalPreservationScore(2)
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        BigDecimal result = calculator.calculateFinancialValueScore(input);

        System.out.println("투자 가치관 일치도 점수: " + result);
        assertEquals(new BigDecimal("16.95"), result);
    }

    @Test
    void 목표_달성_가능성_점수를_계산한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .financialAsset(new BigDecimal("90000000"))
                .monthlyAvailableAmount(new BigDecimal("1000000"))
                .pensionSavingBalance(new BigDecimal("5000000"))
                .irpBalance(BigDecimal.ZERO)
                .pensionAnnualPayment(new BigDecimal("6000000"))
                .irpAnnualPayment(BigDecimal.ZERO)
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .financialAsset(new BigDecimal("180000000"))
                .monthlyAvailableAmount(new BigDecimal("1500000"))
                .pensionSavingBalance(new BigDecimal("20000000"))
                .irpBalance(new BigDecimal("15000000"))
                .pensionAnnualPayment(new BigDecimal("6000000"))
                .irpAnnualPayment(new BigDecimal("3000000"))
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .firstGoalType("HOUSING")
                .targetAmount(new BigDecimal("350000000"))
                .targetPeriodMonths(36)
                .build();

        BigDecimal result = calculator.calculateGoalFeasibilityScore(input);

        System.out.println("목표 달성 가능성 점수: " + result);

        assertEquals(new BigDecimal("12.79"), result);
    }

    @Test
    void 절세_활용도_점수를_계산한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .pensionAnnualPayment(new BigDecimal("6000000"))
                .irpAnnualPayment(BigDecimal.ZERO)
                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(new BigDecimal("10000000"))
                .taxEligibilityStatus("ELIGIBLE")
                .isaEligibilityStatus("ELIGIBLE")
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .pensionAnnualPayment(new BigDecimal("6000000"))
                .irpAnnualPayment(new BigDecimal("3000000"))
                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(new BigDecimal("20000000"))
                .taxEligibilityStatus("ELIGIBLE")
                .isaEligibilityStatus("ELIGIBLE")
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        BigDecimal result = calculator.calculateTaxStrategyScore(input);

        System.out.println("절세 활용도 점수: " + result);

        assertEquals(new BigDecimal("8.00"), result);
    }

    @Test
    void 전체_금융_궁합도_점수를_계산한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .age(32)
                .ageGroupAssetMedian(new BigDecimal("93300000"))
                .financialAsset(new BigDecimal("90000000"))

                .annualIncome(new BigDecimal("45000000"))
                .totalDebt(new BigDecimal("20000000"))
                .annualDebtPayment(new BigDecimal("6000000"))

                .financialAssetRatioScore(3)
                .investmentExperienceScore(3)
                .financialKnowledgeScore(3)
                .capitalPreservationScore(4)

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

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .age(33)
                .ageGroupAssetMedian(new BigDecimal("93300000"))
                .financialAsset(new BigDecimal("180000000"))

                .annualIncome(new BigDecimal("75000000"))
                .totalDebt(new BigDecimal("40000000"))
                .annualDebtPayment(new BigDecimal("10000000"))

                .financialAssetRatioScore(4)
                .investmentExperienceScore(4)
                .financialKnowledgeScore(4)
                .capitalPreservationScore(2)

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

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .firstGoalType("HOUSING")
                .targetAmount(new BigDecimal("350000000"))
                .targetPeriodMonths(36)
                .build();

        MatchCalculationResult result = calculator.calculate(input);

        System.out.println(result);

        assertEquals(
                new BigDecimal("18.62"),
                result.getAssetStabilityScore()
        );

        assertEquals(
                new BigDecimal("14.00"),
                result.getDebtRepaymentScore()
        );

        assertEquals(
                new BigDecimal("16.95"),
                result.getFinancialValueScore()
        );

        assertEquals(
                new BigDecimal("12.79"),
                result.getGoalFeasibilityScore()
        );

        assertEquals(
                new BigDecimal("8.00"),
                result.getTaxStrategyScore()
        );

        assertEquals(
                new BigDecimal("71"),
                result.getTotalScore()
        );
    }

    @Test
    void 절세가_미산출이면_90점을_100점으로_환산한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .age(32)
                .ageGroupAssetMedian(new BigDecimal("93300000"))
                .financialAsset(new BigDecimal("90000000"))

                .annualIncome(new BigDecimal("45000000"))
                .totalDebt(new BigDecimal("20000000"))
                .annualDebtPayment(new BigDecimal("6000000"))

                .financialAssetRatioScore(3)
                .investmentExperienceScore(3)
                .financialKnowledgeScore(3)
                .capitalPreservationScore(4)

                .monthlyAvailableAmount(new BigDecimal("1000000"))
                .pensionSavingBalance(new BigDecimal("5000000"))
                .irpBalance(BigDecimal.ZERO)
                .pensionAnnualPayment(new BigDecimal("6000000"))
                .irpAnnualPayment(BigDecimal.ZERO)

                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(new BigDecimal("10000000"))
                .taxEligibilityStatus("INELIGIBLE")
                .isaEligibilityStatus("INELIGIBLE")
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .age(33)
                .ageGroupAssetMedian(new BigDecimal("93300000"))
                .financialAsset(new BigDecimal("180000000"))

                .annualIncome(new BigDecimal("75000000"))
                .totalDebt(new BigDecimal("40000000"))
                .annualDebtPayment(new BigDecimal("10000000"))

                .financialAssetRatioScore(4)
                .investmentExperienceScore(4)
                .financialKnowledgeScore(4)
                .capitalPreservationScore(2)

                .monthlyAvailableAmount(new BigDecimal("1500000"))
                .pensionSavingBalance(new BigDecimal("20000000"))
                .irpBalance(new BigDecimal("15000000"))
                .pensionAnnualPayment(new BigDecimal("6000000"))
                .irpAnnualPayment(new BigDecimal("3000000"))

                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(new BigDecimal("20000000"))
                .taxEligibilityStatus("INELIGIBLE")
                .isaEligibilityStatus("INELIGIBLE")
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .firstGoalType("HOUSING")
                .targetAmount(new BigDecimal("350000000"))
                .targetPeriodMonths(36)
                .build();

        MatchCalculationResult result = calculator.calculate(input);

        System.out.println("절세 미산출 결과: " + result);

        assertEquals(
                new BigDecimal("18.62"),
                result.getAssetStabilityScore()
        );

        assertEquals(
                new BigDecimal("14.00"),
                result.getDebtRepaymentScore()
        );

        assertEquals(
                new BigDecimal("16.95"),
                result.getFinancialValueScore()
        );

        assertEquals(
                new BigDecimal("12.79"),
                result.getGoalFeasibilityScore()
        );

        assertEquals(
                new BigDecimal("0.00"),
                result.getTaxStrategyScore()
        );

        assertFalse(result.isTaxStrategyCalculated());

        assertEquals(
                new BigDecimal("70"),
                result.getTotalScore()
        );
    }

    //-------------예외처리 테스트 ------------------------
    //
    //
    //금융 자산 경계값 테스트
    @Test
    void 서로_다른_연령대_중앙값을_합산해_금융자산_점수를_계산한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .ageGroupAssetMedian(new BigDecimal("50000000"))
                .financialAsset(new BigDecimal("50000000"))
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .ageGroupAssetMedian(new BigDecimal("100000000"))
                .financialAsset(new BigDecimal("100000000"))
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        BigDecimal result =
                calculator.calculateAssetStabilityScore(input);

        assertEquals(new BigDecimal("15.00"), result);
    }

    @Test
    void 금융자산이_모두_0원이면_0점이다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .ageGroupAssetMedian(new BigDecimal("50000000"))
                .financialAsset(BigDecimal.ZERO)
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .ageGroupAssetMedian(new BigDecimal("100000000"))
                .financialAsset(BigDecimal.ZERO)
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        assertEquals(
                new BigDecimal("0.00"),
                calculator.calculateAssetStabilityScore(input)
        );
    }

    @Test
    void 금융자산_중앙값이_0이면_예외가_발생한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .ageGroupAssetMedian(BigDecimal.ZERO)
                .financialAsset(new BigDecimal("10000000"))
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .ageGroupAssetMedian(new BigDecimal("50000000"))
                .financialAsset(new BigDecimal("10000000"))
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateAssetStabilityScore(input)
        );
    }
    @Test
    void 부채가_없으면_부채_점수는_20점이다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .annualIncome(BigDecimal.ZERO)
                .totalDebt(BigDecimal.ZERO)
                .annualDebtPayment(BigDecimal.ZERO)
                .financialAsset(BigDecimal.ZERO)
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .annualIncome(new BigDecimal("50000000"))
                .totalDebt(BigDecimal.ZERO)
                .annualDebtPayment(BigDecimal.ZERO)
                .financialAsset(new BigDecimal("100000000"))
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        assertEquals(
                new BigDecimal("20.00"),
                calculator.calculateDebtRepaymentScore(input)
        );
    }

    @Test
    void 부채는_있지만_소득과_금융자산이_없으면_0점이다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .annualIncome(BigDecimal.ZERO)
                .totalDebt(new BigDecimal("10000000"))
                .annualDebtPayment(new BigDecimal("1000000"))
                .financialAsset(BigDecimal.ZERO)
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .annualIncome(BigDecimal.ZERO)
                .totalDebt(new BigDecimal("20000000"))
                .annualDebtPayment(new BigDecimal("2000000"))
                .financialAsset(BigDecimal.ZERO)
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        assertEquals(
                new BigDecimal("0.00"),
                calculator.calculateDebtRepaymentScore(input)
        );
    }
    @Test
    void 두_사람의_가치관이_완전히_같으면_25점이다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .financialAssetRatioScore(3)
                .investmentExperienceScore(3)
                .financialKnowledgeScore(3)
                .capitalPreservationScore(3)
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .financialAssetRatioScore(3)
                .investmentExperienceScore(3)
                .financialKnowledgeScore(3)
                .capitalPreservationScore(3)
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        assertEquals(
                new BigDecimal("25.00"),
                calculator.calculateFinancialValueScore(input)
        );
    }

    @Test
    void 가치관_응답점수가_범위를_벗어나면_예외가_발생한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .financialAssetRatioScore(0)
                .investmentExperienceScore(3)
                .financialKnowledgeScore(3)
                .capitalPreservationScore(3)
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .financialAssetRatioScore(3)
                .investmentExperienceScore(3)
                .financialKnowledgeScore(3)
                .capitalPreservationScore(3)
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateFinancialValueScore(input)
        );
    }
    @Test
    void 은퇴_목표에서는_연금잔액을_제외하지_않는다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .financialAsset(new BigDecimal("50000000"))
                .monthlyAvailableAmount(BigDecimal.ZERO)
                .pensionSavingBalance(new BigDecimal("20000000"))
                .irpBalance(BigDecimal.ZERO)
                .pensionAnnualPayment(BigDecimal.ZERO)
                .irpAnnualPayment(BigDecimal.ZERO)
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .financialAsset(new BigDecimal("50000000"))
                .monthlyAvailableAmount(BigDecimal.ZERO)
                .pensionSavingBalance(new BigDecimal("20000000"))
                .irpBalance(BigDecimal.ZERO)
                .pensionAnnualPayment(BigDecimal.ZERO)
                .irpAnnualPayment(BigDecimal.ZERO)
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .firstGoalType("RETIREMENT")
                .targetAmount(new BigDecimal("100000000"))
                .targetPeriodMonths(1)
                .build();

        assertEquals(
                new BigDecimal("15.00"),
                calculator.calculateGoalFeasibilityScore(input)
        );
    }

    @Test
    void 목표금액이_0이면_예외가_발생한다() {
        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(MemberCalculationInput.builder().build())
                .memberB(MemberCalculationInput.builder().build())
                .firstGoalType("HOUSING")
                .targetAmount(BigDecimal.ZERO)
                .targetPeriodMonths(12)
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateGoalFeasibilityScore(input)
        );
    }
    @Test
    void 한_사람만_연금_평가대상이고_한도를_모두_활용하면_10점이다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .pensionAnnualPayment(new BigDecimal("6000000"))
                .irpAnnualPayment(new BigDecimal("3000000"))
                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(BigDecimal.ZERO)
                .taxEligibilityStatus("ELIGIBLE")
                .isaEligibilityStatus("INELIGIBLE")
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .pensionAnnualPayment(BigDecimal.ZERO)
                .irpAnnualPayment(BigDecimal.ZERO)
                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(BigDecimal.ZERO)
                .taxEligibilityStatus("INELIGIBLE")
                .isaEligibilityStatus("INELIGIBLE")
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        assertEquals(
                new BigDecimal("10.00"),
                calculator.calculateTaxStrategyScore(input)
        );
    }

    @Test
    void 절세_평가상태가_UNKNOWN이면_예외가_발생한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .pensionAnnualPayment(BigDecimal.ZERO)
                .irpAnnualPayment(BigDecimal.ZERO)
                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(BigDecimal.ZERO)
                .taxEligibilityStatus("UNKNOWN")
                .isaEligibilityStatus("ELIGIBLE")
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .pensionAnnualPayment(BigDecimal.ZERO)
                .irpAnnualPayment(BigDecimal.ZERO)
                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(BigDecimal.ZERO)
                .taxEligibilityStatus("ELIGIBLE")
                .isaEligibilityStatus("ELIGIBLE")
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateTaxStrategyScore(input)
        );
    }

    @Test
    void 절세_납입액이_음수이면_예외가_발생한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .pensionAnnualPayment(new BigDecimal("-1"))
                .irpAnnualPayment(BigDecimal.ZERO)
                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(BigDecimal.ZERO)
                .taxEligibilityStatus("ELIGIBLE")
                .isaEligibilityStatus("ELIGIBLE")
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .pensionAnnualPayment(BigDecimal.ZERO)
                .irpAnnualPayment(BigDecimal.ZERO)
                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(BigDecimal.ZERO)
                .taxEligibilityStatus("ELIGIBLE")
                .isaEligibilityStatus("ELIGIBLE")
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateTaxStrategyScore(input)
        );
    }

    @Test
    void 절세_입력값이_null이면_예외가_발생한다() {
        MemberCalculationInput memberA = MemberCalculationInput.builder()
                .pensionAnnualPayment(null)
                .irpAnnualPayment(BigDecimal.ZERO)
                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(BigDecimal.ZERO)
                .taxEligibilityStatus("ELIGIBLE")
                .isaEligibilityStatus("ELIGIBLE")
                .build();

        MemberCalculationInput memberB = MemberCalculationInput.builder()
                .pensionAnnualPayment(BigDecimal.ZERO)
                .irpAnnualPayment(BigDecimal.ZERO)
                .dcAnnualPayment(BigDecimal.ZERO)
                .isaAnnualDeposit(BigDecimal.ZERO)
                .taxEligibilityStatus("ELIGIBLE")
                .isaEligibilityStatus("ELIGIBLE")
                .build();

        MatchCalculationInput input = MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateTaxStrategyScore(input)
        );
    }
}
