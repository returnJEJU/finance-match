package com.financematch.asset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.asset.domain.AssetCategory;
import com.financematch.asset.domain.MyDataAsset;
import com.financematch.asset.domain.MyDataLoan;
import com.financematch.asset.domain.MyDataSnapshot;
import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MatchCalculationResult;
import com.financematch.match.calculator.MatchCalculator;
import com.financematch.match.calculator.MemberCalculationInput;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SimpleMyDataProviderTest {

    private final SimpleMyDataProvider provider = new SimpleMyDataProvider();

    @Test
    void returnsDebtFreePersonalScenarioForOddMember() {
        MyDataSnapshot snapshot = provider.fetch(1L);

        assertEquals(new BigDecimal("84200000"), totalAsset(snapshot));
        assertEquals(
                new BigDecimal("12630000"),
                amountOf(snapshot, AssetCategory.BANK_CHECKING));
        assertTrue(snapshot.loans().isEmpty());
        assertFalse(snapshot.pensionIsa().hasIrp());
        assertTrue(snapshot.pensionIsa().hasIsa());
        assertTrue(snapshot.pensionIsa().hasPensionSaving());
        assertEquals(
                new BigDecimal("4800000"),
                snapshot.pensionIsa().pensionAnnualPayment());
        assertEquals(
                new BigDecimal("6000000"),
                snapshot.pensionIsa().isaAnnualDeposit());
    }

    @Test
    void returnsHousingLoanPersonalScenarioForEvenMember() {
        MyDataSnapshot snapshot = provider.fetch(2L);

        assertEquals(new BigDecimal("63000000"), totalAsset(snapshot));
        assertEquals(new BigDecimal("35000000"), totalDebt(snapshot));
        assertEquals(new BigDecimal("3600000"), annualDebtPayment(snapshot));
        assertEquals(new BigDecimal("4.15"), snapshot.loans().get(0).interestRate());
        assertFalse(snapshot.loans().get(0).highRateDebt());
        assertTrue(snapshot.pensionIsa().hasIrp());
        assertFalse(snapshot.pensionIsa().hasIsa());
    }

    @Test
    void calculatesRealisticNewlywedScoreFromTwoPersonalScenarios() {
        MyDataSnapshot memberASnapshot = provider.fetch(1L);
        MyDataSnapshot memberBSnapshot = provider.fetch(2L);
        MatchCalculationInput input =
                MatchCalculationInput.builder()
                        .memberA(
                                memberInput(
                                        memberASnapshot,
                                        new BigDecimal("45000000"),
                                        new BigDecimal("1000000"),
                                        3,
                                        3,
                                        3,
                                        4))
                        .memberB(
                                memberInput(
                                        memberBSnapshot,
                                        new BigDecimal("75000000"),
                                        new BigDecimal("1500000"),
                                        4,
                                        4,
                                        4,
                                        2))
                        .firstGoalType("HOUSING")
                        .targetAmount(new BigDecimal("350000000"))
                        .targetPeriodMonths(36)
                        .build();

        MatchCalculationResult result = new MatchCalculator().calculate(input);

        assertTrue(result.getAssetStabilityScore().compareTo(new BigDecimal("10.00")) > 0);
        assertTrue(result.getAssetStabilityScore().compareTo(new BigDecimal("15.00")) < 0);
        assertTrue(result.getDebtRepaymentScore().compareTo(new BigDecimal("15.00")) > 0);
        assertTrue(result.getDebtRepaymentScore().compareTo(new BigDecimal("20.00")) < 0);
        assertTrue(result.getGoalFeasibilityScore().compareTo(new BigDecimal("8.00")) > 0);
        assertTrue(result.getGoalFeasibilityScore().compareTo(new BigDecimal("12.00")) < 0);
        assertTrue(result.getTaxStrategyScore().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.getTaxStrategyScore().compareTo(new BigDecimal("10.00")) < 0);
    }

    private MemberCalculationInput memberInput(
            MyDataSnapshot snapshot,
            BigDecimal annualIncome,
            BigDecimal monthlyAvailableAmount,
            int financialAssetRatioScore,
            int investmentExperienceScore,
            int financialKnowledgeScore,
            int capitalPreservationScore) {
        return MemberCalculationInput.builder()
                .age(32)
                .ageGroupAssetMedian(new BigDecimal("93300000"))
                .financialAsset(totalAsset(snapshot))
                .annualIncome(annualIncome)
                .totalDebt(totalDebt(snapshot))
                .annualDebtPayment(annualDebtPayment(snapshot))
                .financialAssetRatioScore(financialAssetRatioScore)
                .investmentExperienceScore(investmentExperienceScore)
                .financialKnowledgeScore(financialKnowledgeScore)
                .capitalPreservationScore(capitalPreservationScore)
                .monthlyAvailableAmount(monthlyAvailableAmount)
                .pensionSavingBalance(snapshot.pensionIsa().pensionSavingBalance())
                .irpBalance(snapshot.pensionIsa().irpBalance())
                .pensionAnnualPayment(snapshot.pensionIsa().pensionAnnualPayment())
                .irpAnnualPayment(snapshot.pensionIsa().irpAnnualPayment())
                .dcAnnualPayment(snapshot.pensionIsa().dcAnnualPayment())
                .isaAnnualDeposit(snapshot.pensionIsa().isaAnnualDeposit())
                .taxEligibilityStatus(snapshot.pensionIsa().taxEligibilityStatus())
                .isaEligibilityStatus(snapshot.pensionIsa().isaEligibilityStatus())
                .build();
    }

    private BigDecimal totalAsset(MyDataSnapshot snapshot) {
        return snapshot.assets().stream()
                .map(MyDataAsset::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalDebt(MyDataSnapshot snapshot) {
        return snapshot.loans().stream()
                .map(MyDataLoan::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal annualDebtPayment(MyDataSnapshot snapshot) {
        return snapshot.loans().stream()
                .map(MyDataLoan::annualPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal amountOf(
            MyDataSnapshot snapshot,
            AssetCategory category) {
        return snapshot.assets().stream()
                .filter(asset -> asset.category() == category)
                .map(MyDataAsset::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
