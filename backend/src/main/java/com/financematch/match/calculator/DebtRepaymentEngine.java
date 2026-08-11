package com.financematch.match.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class DebtRepaymentEngine {

    private static final double MEMBER_WEIGHT = 0.3;
    private static final double COUPLE_WEIGHT = 0.4;
    private static final double DEBT_MAX_SCORE = 20.0;

    private static final double DSR_WEIGHT = 0.7;
    private static final double DEBT_RATIO_WEIGHT = 0.3;
    private static final double DSR_LIMIT = 0.4;

    /**
     * 부채 상환 안정성 점수를 계산한다.
     * 최대 점수는 20점이다.
     */
    public BigDecimal calculateScore(MatchCalculationInput input) {
        validateInput(input);

        MemberCalculationInput memberA = input.getMemberA();
        MemberCalculationInput memberB = input.getMemberB();

        double memberAScore = calculateMemberScore(memberA);
        double memberBScore = calculateMemberScore(memberB);

        BigDecimal coupleTotalDebt = memberA.getTotalDebt()
                .add(memberB.getTotalDebt());

        BigDecimal coupleAnnualDebtPayment =
                memberA.getAnnualDebtPayment()
                        .add(memberB.getAnnualDebtPayment());

        BigDecimal coupleAnnualIncome = memberA.getAnnualIncome()
                .add(memberB.getAnnualIncome());

        BigDecimal coupleFinancialAsset =
                memberA.getFinancialAsset()
                        .add(memberB.getFinancialAsset());

        double coupleScore = calculateDebtBaseScore(
                coupleTotalDebt,
                coupleAnnualDebtPayment,
                coupleAnnualIncome,
                coupleFinancialAsset
        );

        double weightedScore =
                MEMBER_WEIGHT * memberAScore
                        + MEMBER_WEIGHT * memberBScore
                        + COUPLE_WEIGHT * coupleScore;

        double finalScore =
                weightedScore * (DEBT_MAX_SCORE / 100.0);

        return round(finalScore);
    }

    /**
     * 회원 한 명의 부채 원점수를 계산한다.
     * 반환 범위는 0~100점이다.
     */
    public double calculateMemberScore(
            MemberCalculationInput member
    ) {
        if (member == null) {
            throw new IllegalArgumentException(
                    "회원의 부채 계산 입력값이 필요합니다."
            );
        }

        return calculateDebtBaseScore(
                member.getTotalDebt(),
                member.getAnnualDebtPayment(),
                member.getAnnualIncome(),
                member.getFinancialAsset()
        );
    }

    private double calculateDebtBaseScore(
            BigDecimal totalDebt,
            BigDecimal annualDebtPayment,
            BigDecimal annualIncome,
            BigDecimal financialAsset
    ) {
        if (totalDebt == null
                || annualDebtPayment == null
                || annualIncome == null
                || financialAsset == null) {
            throw new IllegalArgumentException(
                    "부채 계산 입력값이 필요합니다."
            );
        }

        double debt = Math.max(
                totalDebt.doubleValue(),
                0.0
        );

        double payment = Math.max(
                annualDebtPayment.doubleValue(),
                0.0
        );

        double income = Math.max(
                annualIncome.doubleValue(),
                0.0
        );

        double asset = Math.max(
                financialAsset.doubleValue(),
                0.0
        );

        // 부채가 없으면 상환 부담이 없으므로 100점
        if (debt == 0.0) {
            return 100.0;
        }

        double normalizedDsr;

        if (income == 0.0) {
            normalizedDsr = 1.0;
        } else {
            normalizedDsr = Math.min(
                    payment / (DSR_LIMIT * income),
                    1.0
            );
        }

        double normalizedDebt;

        if (asset == 0.0) {
            normalizedDebt = 1.0;
        } else {
            normalizedDebt = Math.min(
                    debt / asset,
                    1.0
            );
        }

        double risk =
                DSR_WEIGHT * normalizedDsr
                        + DEBT_RATIO_WEIGHT * normalizedDebt;

        return 100.0 * (1.0 - risk);
    }

    private void validateInput(MatchCalculationInput input) {
        if (input == null
                || input.getMemberA() == null
                || input.getMemberB() == null) {
            throw new IllegalArgumentException(
                    "두 회원의 부채 계산 입력값이 필요합니다."
            );
        }
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}