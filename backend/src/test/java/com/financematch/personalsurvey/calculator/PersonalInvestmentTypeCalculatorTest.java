package com.financematch.personalsurvey.calculator;

import com.financematch.invitation.domain.GoalType;
import com.financematch.personalsurvey.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PersonalInvestmentTypeCalculatorTest {

    private static final LocalDate ASSESSMENT_DATE = LocalDate.of(2026, 7, 30);

    private final PersonalInvestmentTypeCalculator calculator = new PersonalInvestmentTypeCalculator();

    @Test // 복수 투자경험 중 가장 높은 경험 점수가 최종 성향 계산에 반영되는지 확인
    void usesHighestInvestmentExperienceScore() {
        PersonalInvestmentCalculationInput lowRiskOnly =
                PersonalInvestmentCalculationInput.builder()
                        .birthDate(LocalDate.of(1996, 1, 1))
                        .annualIncome(100_000_000L)
                        .financialAssetRatio(FinancialAssetRatio.UNDER_80)
                        .investmentExperiences(Set.of(InvestmentExperience.LOW_RISK))
                        .financialKnowledge(FinancialKnowledge.MEDIUM)
                        .capitalPreservationAttitude(CapitalPreservationAttitude.UNDER_50)
                        .firstGoalType(GoalType.HOUSING)
                        .targetPeriodMonths(6)
                        .build();

        PersonalInvestmentCalculationInput includingHighRisk =
                PersonalInvestmentCalculationInput.builder()
                        .birthDate(LocalDate.of(1996, 1, 1))
                        .annualIncome(100_000_000L)
                        .financialAssetRatio(FinancialAssetRatio.UNDER_80)
                        .investmentExperiences(Set.of(InvestmentExperience.LOW_RISK, InvestmentExperience.HIGH_RISK))
                        .financialKnowledge(FinancialKnowledge.MEDIUM)
                        .capitalPreservationAttitude(CapitalPreservationAttitude.UNDER_50)
                        .firstGoalType(GoalType.HOUSING)
                        .targetPeriodMonths(6)
                        .build();

        assertEquals(PersonalInvestmentType.NEUTRAL,
                calculator.calculate(lowRiskOnly, ASSESSMENT_DATE));

        assertEquals(PersonalInvestmentType.AGGRESSIVE,
                calculator.calculate(includingHighRisk, ASSESSMENT_DATE));
    }

    @Test // 원금보존 추구 응답은 환산점수와 관계없이 안정형으로 강제 분류되는지 확인
    void returnsStableForCapitalPreservation() {
        PersonalInvestmentCalculationInput input = highestRiskInput(
                        CapitalPreservationAttitude.ZERO,
                        GoalType.INVESTMENT);

        assertEquals(PersonalInvestmentType.STABLE,
                calculator.calculate(input, ASSESSMENT_DATE));
    }

    @Test // 10% 이내 손실 감내 응답은 계산 결과를 위험중립형 이하로 제한하는지 확인
    void capsAtNeutralForTenPercentLossTolerance() {
        PersonalInvestmentCalculationInput input = highestRiskInput(
                        CapitalPreservationAttitude.UNDER_10,
                        GoalType.INVESTMENT);

        assertEquals(PersonalInvestmentType.NEUTRAL,
                calculator.calculate(input, ASSESSMENT_DATE));
    }

    @Test // 1순위 목표가 단기운용이면 계산 결과를 위험중립형 이하로 제한하는지 확인
    void capsAtNeutralForShortTermGoal() {
        PersonalInvestmentCalculationInput input = highestRiskInput(
                        CapitalPreservationAttitude.FULL,
                        GoalType.SHORT_TERM);

        assertEquals(PersonalInvestmentType.NEUTRAL,
                calculator.calculate(input, ASSESSMENT_DATE));
    }

    @Test // 모든 문항에서 최고점을 선택하면 100점으로 환산되어 공격투자형이 되는지 확인
    void returnsVeryAggressiveForMaximumScore() {
        PersonalInvestmentCalculationInput input = highestRiskInput(
                        CapitalPreservationAttitude.FULL,
                        GoalType.INVESTMENT);

        assertEquals(PersonalInvestmentType.VERY_AGGRESSIVE,
                calculator.calculate(input, ASSESSMENT_DATE));
    }

    // 공통 고득점 입력 (CapitalPreservationAttitude, GoalType 제외)
    // 강제 분류 조건만 비교할 수 있도록 나머지 문항은 최고점으로 구성
    private PersonalInvestmentCalculationInput highestRiskInput(
            CapitalPreservationAttitude attitude,
            GoalType goalType) {

        return PersonalInvestmentCalculationInput.builder()
                // 평가일 기준 30세 → 2.5점
                .birthDate(LocalDate.of(1996, 1, 1))
                // 3억원 초과 → 8점
                .annualIncome(300_000_001L)
                // 80% 초과 → 2.5점
                .financialAssetRatio(FinancialAssetRatio.OVER_80)
                // 최고위험 경험 → 5.5점
                .investmentExperiences(Set.of(InvestmentExperience.HIGH_RISK))
                // 매우 높음 → 5.5점
                .financialKnowledge(FinancialKnowledge.VERY_HIGH)
                .capitalPreservationAttitude(attitude)
                .firstGoalType(goalType)
                // 3년 이상 → 2.5점
                .targetPeriodMonths(36)
                .build();
    }

    @Test // 강제 조건 없이 환산점수가 안정추구형 구간에 속하는지 확인
    void classifiesStableSeekingByScore() {
        PersonalInvestmentCalculationInput input =
                PersonalInvestmentCalculationInput.builder()
                        .birthDate(LocalDate.of(1966, 1, 1))
                        .annualIncome(30_000_000L)
                        .financialAssetRatio(FinancialAssetRatio.UNDER_10)
                        .investmentExperiences(Set.of(InvestmentExperience.LOW_RISK))
                        .financialKnowledge(FinancialKnowledge.VERY_LOW)
                        .capitalPreservationAttitude(CapitalPreservationAttitude.UNDER_20)
                        .firstGoalType(GoalType.HOUSING)
                        .targetPeriodMonths(3)
                        .build();

        assertEquals(PersonalInvestmentType.STABLE_SEEKING,
                calculator.calculate(input, ASSESSMENT_DATE));
    }

    @Test // 강제 조건 없이 환산점수가 적극투자형 구간에 속하는지 확인
    void classifiesAggressiveByScore() {
        PersonalInvestmentCalculationInput input =
                PersonalInvestmentCalculationInput.builder()
                        .birthDate(LocalDate.of(1996, 1, 1))
                        .annualIncome(100_000_000L)
                        .financialAssetRatio(FinancialAssetRatio.UNDER_80)
                        .investmentExperiences(Set.of(InvestmentExperience.MODERATE_RISK))
                        .financialKnowledge(FinancialKnowledge.MEDIUM)
                        .capitalPreservationAttitude(CapitalPreservationAttitude.UNDER_50)
                        .firstGoalType(GoalType.HOUSING)
                        .targetPeriodMonths(12)
                        .build();

        assertEquals(PersonalInvestmentType.AGGRESSIVE,
                calculator.calculate(input, ASSESSMENT_DATE));
    }

}