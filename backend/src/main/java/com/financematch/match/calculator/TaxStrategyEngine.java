package com.financematch.match.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class TaxStrategyEngine {

    private static final double TAX_MAX_SCORE = 10.0;
    private static final double PENSION_MAX_SCORE = 6.0;
    private static final double ISA_MAX_SCORE = 4.0;

    private static final double PENSION_SAVING_LIMIT =
            6_000_000.0;

    private static final double PENSION_TOTAL_LIMIT =
            9_000_000.0;

    private static final double ISA_ANNUAL_LIMIT =
            20_000_000.0;

    /**
     * report 영역에서 한도 금액을 표시할 때 사용하는 공개 상수다.
     */
    public static final BigDecimal PENSION_SAVING_ANNUAL_LIMIT =
            BigDecimal.valueOf((long) PENSION_SAVING_LIMIT);

    public static final BigDecimal PENSION_IRP_ANNUAL_LIMIT =
            BigDecimal.valueOf((long) PENSION_TOTAL_LIMIT);

    public static final BigDecimal ISA_ANNUAL_LIMIT_AMOUNT =
            BigDecimal.valueOf((long) ISA_ANNUAL_LIMIT);

    private static final String ELIGIBLE = "ELIGIBLE";
    private static final String INELIGIBLE = "INELIGIBLE";

    /**
     * 절세 활용도 점수를 계산한다.
     * 최대 점수는 10점이다.
     */
    public BigDecimal calculateScore(MatchCalculationInput input) {
        validateInput(input);

        MemberCalculationInput[] members = {
                input.getMemberA(),
                input.getMemberB()
        };

        double pensionUtilizationSum = 0.0;
        int pensionEligibleCount = 0;

        double isaUtilizationSum = 0.0;
        int isaEligibleCount = 0;

        for (MemberCalculationInput member : members) {
            validateTaxMemberInput(member);

            if (isEligible(
                    member.getTaxEligibilityStatus(),
                    "연금"
            )) {
                pensionUtilizationSum +=
                        calculatePensionUtilization(member);

                pensionEligibleCount++;
            }

            if (isEligible(
                    member.getIsaEligibilityStatus(),
                    "ISA"
            )) {
                isaUtilizationSum +=
                        calculateIsaUtilization(member);

                isaEligibleCount++;
            }
        }

        boolean pensionApplicable =
                pensionEligibleCount > 0;

        boolean isaApplicable =
                isaEligibleCount > 0;

        double pensionScore = 0.0;

        if (pensionApplicable) {
            double couplePensionUtilization =
                    pensionUtilizationSum
                            / pensionEligibleCount;

            pensionScore =
                    couplePensionUtilization
                            * PENSION_MAX_SCORE;
        }

        double isaScore = 0.0;

        if (isaApplicable) {
            double coupleIsaUtilization =
                    isaUtilizationSum
                            / isaEligibleCount;

            isaScore =
                    coupleIsaUtilization
                            * ISA_MAX_SCORE;
        }

        double applicableMaxScore =
                (pensionApplicable
                        ? PENSION_MAX_SCORE
                        : 0.0)
                        + (isaApplicable
                        ? ISA_MAX_SCORE
                        : 0.0);

        // 연금과 ISA 모두 평가 대상이 아니면 미산출
        if (applicableMaxScore == 0.0) {
            return BigDecimal.ZERO.setScale(2);
        }

        double finalScore =
                TAX_MAX_SCORE
                        * ((pensionScore + isaScore)
                        / applicableMaxScore);

        return round(finalScore);
    }

    /**
     * 절세 활용도 점수가 총점 계산 대상인지 확인한다.
     */
    public boolean isCalculated(MatchCalculationInput input) {
        validateInput(input);

        MemberCalculationInput memberA = input.getMemberA();
        MemberCalculationInput memberB = input.getMemberB();

        return ELIGIBLE.equals(
                memberA.getTaxEligibilityStatus()
        )
                || ELIGIBLE.equals(
                memberB.getTaxEligibilityStatus()
        )
                || ELIGIBLE.equals(
                memberA.getIsaEligibilityStatus()
        )
                || ELIGIBLE.equals(
                memberB.getIsaEligibilityStatus()
        );
    }

    private double calculatePensionUtilization(
            MemberCalculationInput member
    ) {
        double pensionSavingPayment = Math.max(
                member.getPensionAnnualPayment().doubleValue(),
                0.0
        );

        double irpPayment = Math.max(
                member.getIrpAnnualPayment().doubleValue(),
                0.0
        );

        double dcPayment = Math.max(
                member.getDcAnnualPayment().doubleValue(),
                0.0
        );

        double recognizedPensionSaving = Math.min(
                pensionSavingPayment,
                PENSION_SAVING_LIMIT
        );

        double recognizedTotalPayment = Math.min(
                recognizedPensionSaving
                        + irpPayment
                        + dcPayment,
                PENSION_TOTAL_LIMIT
        );

        return recognizedTotalPayment
                / PENSION_TOTAL_LIMIT;
    }

    private double calculateIsaUtilization(
            MemberCalculationInput member
    ) {
        double isaDeposit = Math.max(
                member.getIsaAnnualDeposit().doubleValue(),
                0.0
        );

        return Math.min(
                isaDeposit / ISA_ANNUAL_LIMIT,
                1.0
        );
    }

    private boolean isEligible(
            String eligibilityStatus,
            String fieldName
    ) {
        if (ELIGIBLE.equals(eligibilityStatus)) {
            return true;
        }

        if (INELIGIBLE.equals(eligibilityStatus)) {
            return false;
        }

        throw new IllegalArgumentException(
                fieldName
                        + " 평가 대상 여부를 확인할 수 없습니다."
        );
    }

    private void validateInput(MatchCalculationInput input) {
        if (input == null
                || input.getMemberA() == null
                || input.getMemberB() == null) {
            throw new IllegalArgumentException(
                    "두 회원의 절세 계산 입력값이 필요합니다."
            );
        }
    }

    private void validateTaxMemberInput(
            MemberCalculationInput member
    ) {
        if (member.getPensionAnnualPayment() == null
                || member.getIrpAnnualPayment() == null
                || member.getDcAnnualPayment() == null
                || member.getIsaAnnualDeposit() == null
                || member.getTaxEligibilityStatus() == null
                || member.getIsaEligibilityStatus() == null) {
            throw new IllegalArgumentException(
                    "회원의 절세 계산 입력값이 필요합니다."
            );
        }

        if (member.getPensionAnnualPayment()
                .compareTo(BigDecimal.ZERO) < 0
                || member.getIrpAnnualPayment()
                .compareTo(BigDecimal.ZERO) < 0
                || member.getDcAnnualPayment()
                .compareTo(BigDecimal.ZERO) < 0
                || member.getIsaAnnualDeposit()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "절세 납입액은 0보다 작을 수 없습니다."
            );
        }
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}