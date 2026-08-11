package com.financematch.match.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class GoalFeasibilityEngine {

    private static final double GOAL_MAX_SCORE = 20.0;
    private static final double ANNUAL_RETURN_RATE = 0.03;

    private static final int MONTHS_IN_YEAR = 12;

    private static final String RETIREMENT_GOAL_TYPE =
            "RETIREMENT";

    /**
     * 목표 달성 가능성 점수를 계산한다.
     * 최대 점수는 20점이다.
     */
    public BigDecimal calculateScore(MatchCalculationInput input) {
        validateGoalInput(input);

        double futureAsset = calculateFutureAsset(input);
        double targetAmount = input.getTargetAmount().doubleValue();

        double achievementRate = Math.min(
                futureAsset / targetAmount,
                1.0
        );

        double finalScore =
                GOAL_MAX_SCORE * achievementRate;

        return round(finalScore);
    }

    /**
     * 목표 기간 후 예상 자산을 계산한다.
     * report 영역의 목표 달성 사유 문구에서도 사용한다.
     */
    public BigDecimal calculateExpectedAsset(
            MatchCalculationInput input
    ) {
        validateGoalInput(input);

        return BigDecimal.valueOf(calculateFutureAsset(input))
                .setScale(0, RoundingMode.HALF_UP);
    }

    private double calculateFutureAsset(
            MatchCalculationInput input
    ) {
        MemberCalculationInput memberA = input.getMemberA();
        MemberCalculationInput memberB = input.getMemberB();

        boolean retirementGoal =
                RETIREMENT_GOAL_TYPE.equals(
                        input.getFirstGoalType()
                );

        double currentAvailableAsset =
                calculateCurrentAvailableAsset(
                        memberA,
                        memberB,
                        retirementGoal
                );

        double monthlyAvailableAmount =
                calculateMonthlyAvailableAmount(
                        memberA,
                        memberB,
                        retirementGoal
                );

        double monthlyReturnRate =
                Math.pow(
                        1.0 + ANNUAL_RETURN_RATE,
                        1.0 / MONTHS_IN_YEAR
                ) - 1.0;

        int targetPeriodMonths =
                input.getTargetPeriodMonths();

        double growthFactor =
                Math.pow(
                        1.0 + monthlyReturnRate,
                        targetPeriodMonths
                );

        return currentAvailableAsset * growthFactor
                + monthlyAvailableAmount
                * ((growthFactor - 1.0) / monthlyReturnRate);
    }

    private double calculateCurrentAvailableAsset(
            MemberCalculationInput memberA,
            MemberCalculationInput memberB,
            boolean retirementGoal
    ) {
        double totalFinancialAsset =
                memberA.getFinancialAsset().doubleValue()
                        + memberB.getFinancialAsset().doubleValue();

        if (retirementGoal) {
            return Math.max(totalFinancialAsset, 0.0);
        }

        double pensionAndIrpBalance =
                memberA.getPensionSavingBalance().doubleValue()
                        + memberA.getIrpBalance().doubleValue()
                        + memberB.getPensionSavingBalance().doubleValue()
                        + memberB.getIrpBalance().doubleValue();

        return Math.max(
                totalFinancialAsset - pensionAndIrpBalance,
                0.0
        );
    }

    private double calculateMonthlyAvailableAmount(
            MemberCalculationInput memberA,
            MemberCalculationInput memberB,
            boolean retirementGoal
    ) {
        double totalMonthlyAvailableAmount =
                memberA.getMonthlyAvailableAmount().doubleValue()
                        + memberB.getMonthlyAvailableAmount().doubleValue();

        if (retirementGoal) {
            return Math.max(
                    totalMonthlyAvailableAmount,
                    0.0
            );
        }

        double totalPensionAndIrpAnnualPayment =
                memberA.getPensionAnnualPayment().doubleValue()
                        + memberA.getIrpAnnualPayment().doubleValue()
                        + memberB.getPensionAnnualPayment().doubleValue()
                        + memberB.getIrpAnnualPayment().doubleValue();

        double pensionAndIrpMonthlyPayment =
                totalPensionAndIrpAnnualPayment
                        / MONTHS_IN_YEAR;

        return Math.max(
                totalMonthlyAvailableAmount
                        - pensionAndIrpMonthlyPayment,
                0.0
        );
    }

    private void validateGoalInput(
            MatchCalculationInput input
    ) {
        if (input == null
                || input.getMemberA() == null
                || input.getMemberB() == null) {
            throw new IllegalArgumentException(
                    "두 회원의 목표 계산 입력값이 필요합니다."
            );
        }

        if (input.getFirstGoalType() == null) {
            throw new IllegalArgumentException(
                    "목표 유형이 필요합니다."
            );
        }

        if (input.getTargetAmount() == null
                || input.getTargetAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "목표금액은 0보다 커야 합니다."
            );
        }

        if (input.getTargetPeriodMonths() <= 0) {
            throw new IllegalArgumentException(
                    "목표기간은 1개월 이상이어야 합니다."
            );
        }

        validateGoalMemberInput(input.getMemberA());
        validateGoalMemberInput(input.getMemberB());
    }

    private void validateGoalMemberInput(
            MemberCalculationInput member
    ) {
        if (member.getFinancialAsset() == null
                || member.getMonthlyAvailableAmount() == null
                || member.getPensionSavingBalance() == null
                || member.getIrpBalance() == null
                || member.getPensionAnnualPayment() == null
                || member.getIrpAnnualPayment() == null) {
            throw new IllegalArgumentException(
                    "회원의 목표 계산 입력값이 필요합니다."
            );
        }
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}