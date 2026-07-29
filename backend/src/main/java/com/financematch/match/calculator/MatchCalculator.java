package com.financematch.match.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class MatchCalculator {

    //기본 상수 세팅
    private static final double MEMBER_WEIGHT = 0.3;
    private static final double COUPLE_WEIGHT = 0.4;

    private static final double ASSET_MAX_SCORE = 30.0;
    private static final double DEBT_MAX_SCORE = 20.0;

    private static final double DSR_WEIGHT = 0.7;
    private static final double DEBT_RATIO_WEIGHT = 0.3;
    private static final double DSR_LIMIT = 0.4;

    private static final double GOAL_MAX_SCORE = 15.0;
    private static final double ANNUAL_RETURN_RATE = 0.03;
    private static final int MONTHS_IN_YEAR = 12;
    private static final String RETIREMENT_GOAL_TYPE = "RETIREMENT";

    private static final double TAX_MAX_SCORE = 10.0;
    private static final double PENSION_MAX_SCORE = 6.0;
    private static final double ISA_MAX_SCORE = 4.0;

    private static final double PENSION_SAVING_LIMIT = 6_000_000.0;
    private static final double PENSION_TOTAL_LIMIT = 9_000_000.0;
    private static final double ISA_ANNUAL_LIMIT = 20_000_000.0;

    private static final String ELIGIBLE = "ELIGIBLE";
    private static final String INELIGIBLE = "INELIGIBLE";

    //1. 금융자산
    public BigDecimal calculateAssetStabilityScore(MatchCalculationInput input) {
        MemberCalculationInput memberA = input.getMemberA();
        MemberCalculationInput memberB = input.getMemberB();

        double memberAScore = calculateRelativeAssetScore(
                memberA.getFinancialAsset(),
                memberA.getAgeGroupAssetMedian()
        );

        double memberBScore = calculateRelativeAssetScore(
                memberB.getFinancialAsset(),
                memberB.getAgeGroupAssetMedian()
        );

        BigDecimal coupleFinancialAsset = memberA.getFinancialAsset()
                .add(memberB.getFinancialAsset());

        BigDecimal coupleAssetMedian = memberA.getAgeGroupAssetMedian()
                .add(memberB.getAgeGroupAssetMedian());

        double coupleScore = calculateRelativeAssetScore(
                coupleFinancialAsset,
                coupleAssetMedian
        );

        double weightedScore =
                MEMBER_WEIGHT * memberAScore
                        + MEMBER_WEIGHT * memberBScore
                        + COUPLE_WEIGHT * coupleScore;

        double finalScore = weightedScore * (ASSET_MAX_SCORE / 100.0);

        return round(finalScore);
    }

    private double calculateRelativeAssetScore(
            BigDecimal financialAsset,
            BigDecimal medianFinancialAsset
    ) {
        if (financialAsset == null) {
            throw new IllegalArgumentException("금융자산이 필요합니다.");
        }

        if (medianFinancialAsset == null
                || medianFinancialAsset.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금융자산 중앙값은 0보다 커야 합니다.");
        }

        double asset = Math.max(financialAsset.doubleValue(), 0.0);
        double median = medianFinancialAsset.doubleValue();
        double ratio = asset / median;

        return 100.0 * (1.0 - Math.pow(2.0, -ratio));
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    //2. 부채
    public BigDecimal calculateDebtRepaymentScore(MatchCalculationInput input) {
        MemberCalculationInput memberA = input.getMemberA();
        MemberCalculationInput memberB = input.getMemberB();

        double memberAScore = calculateDebtBaseScore(
                memberA.getTotalDebt(),
                memberA.getAnnualDebtPayment(),
                memberA.getAnnualIncome(),
                memberA.getFinancialAsset()
        );

        double memberBScore = calculateDebtBaseScore(
                memberB.getTotalDebt(),
                memberB.getAnnualDebtPayment(),
                memberB.getAnnualIncome(),
                memberB.getFinancialAsset()
        );

        BigDecimal coupleTotalDebt = memberA.getTotalDebt()
                .add(memberB.getTotalDebt());

        BigDecimal coupleAnnualDebtPayment = memberA.getAnnualDebtPayment()
                .add(memberB.getAnnualDebtPayment());

        BigDecimal coupleAnnualIncome = memberA.getAnnualIncome()
                .add(memberB.getAnnualIncome());

        BigDecimal coupleFinancialAsset = memberA.getFinancialAsset()
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

        double finalScore = weightedScore * (DEBT_MAX_SCORE / 100.0);

        return round(finalScore);
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
            throw new IllegalArgumentException("부채 계산 입력값이 필요합니다.");
        }

        double debt = Math.max(totalDebt.doubleValue(), 0.0);
        double payment = Math.max(annualDebtPayment.doubleValue(), 0.0);
        double income = Math.max(annualIncome.doubleValue(), 0.0);
        double asset = Math.max(financialAsset.doubleValue(), 0.0);

        // 부채가 없으면 상환 부담도 없으므로 100점
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
            normalizedDebt = Math.min(debt / asset, 1.0);
        }

        double risk =
                DSR_WEIGHT * normalizedDsr
                        + DEBT_RATIO_WEIGHT * normalizedDebt;

        return 100.0 * (1.0 - risk);
    }

    //3. 투자 가치관 일치도
    public BigDecimal calculateFinancialValueScore(MatchCalculationInput input) {
        MemberCalculationInput memberA = input.getMemberA();
        MemberCalculationInput memberB = input.getMemberB();

        validateFinancialValueScores(memberA);
        validateFinancialValueScores(memberB);

        double financialAssetRatioScore = calculateSimilarityScore(
                memberA.getFinancialAssetRatioScore(),
                memberB.getFinancialAssetRatioScore(),
                4.0,
                5.0
        );

        double investmentExperienceScore = calculateSimilarityScore(
                memberA.getInvestmentExperienceScore(),
                memberB.getInvestmentExperienceScore(),
                4.0,
                4.0
        );

        double financialKnowledgeScore = calculateSimilarityScore(
                memberA.getFinancialKnowledgeScore(),
                memberB.getFinancialKnowledgeScore(),
                4.0,
                4.0
        );

        double capitalPreservationScore = calculateSimilarityScore(
                memberA.getCapitalPreservationScore(),
                memberB.getCapitalPreservationScore(),
                5.0,
                12.0
        );

        double finalScore =
                financialAssetRatioScore
                        + investmentExperienceScore
                        + financialKnowledgeScore
                        + capitalPreservationScore;

        return round(finalScore);
    }

    private double calculateSimilarityScore(
            int memberAScore,
            int memberBScore,
            double maxDifference,
            double maxScore
    ) {
        int difference = Math.abs(memberAScore - memberBScore);
        double similarity = 1.0 - (difference / maxDifference);

        return maxScore * similarity;
    }

    private void validateFinancialValueScores(MemberCalculationInput member) {
        validateScore(
                member.getFinancialAssetRatioScore(),
                1,
                5,
                "금융자산 비중"
        );

        validateScore(
                member.getInvestmentExperienceScore(),
                1,
                5,
                "투자 경험"
        );

        validateScore(
                member.getFinancialKnowledgeScore(),
                1,
                5,
                "금융상품 이해도"
        );

        validateScore(
                member.getCapitalPreservationScore(),
                1,
                6,
                "원금 보존 태도"
        );
    }

    private void validateScore(
            int score,
            int minimum,
            int maximum,
            String fieldName
    ) {
        if (score < minimum || score > maximum) {
            throw new IllegalArgumentException(
                    fieldName + " 점수는 "
                            + minimum + "점 이상 "
                            + maximum + "점 이하여야 합니다."
            );
        }
    }

    //4. 목표 달성률
    public BigDecimal calculateGoalFeasibilityScore(MatchCalculationInput input) {
        validateGoalInput(input);

        MemberCalculationInput memberA = input.getMemberA();
        MemberCalculationInput memberB = input.getMemberB();

        boolean retirementGoal =
                RETIREMENT_GOAL_TYPE.equals(input.getFirstGoalType());

        double currentAvailableAsset = calculateCurrentAvailableAsset(
                memberA,
                memberB,
                retirementGoal
        );

        double monthlyAvailableAmount = calculateMonthlyAvailableAmount(
                memberA,
                memberB,
                retirementGoal
        );

        double monthlyReturnRate =
                Math.pow(1.0 + ANNUAL_RETURN_RATE, 1.0 / MONTHS_IN_YEAR) - 1.0;

        int targetPeriodMonths = input.getTargetPeriodMonths();

        double growthFactor =
                Math.pow(1.0 + monthlyReturnRate, targetPeriodMonths);

        double futureAsset =
                currentAvailableAsset * growthFactor
                        + monthlyAvailableAmount
                        * ((growthFactor - 1.0) / monthlyReturnRate);

        double targetAmount = input.getTargetAmount().doubleValue();

        double achievementRate = Math.min(futureAsset / targetAmount, 1.0);

        double finalScore = GOAL_MAX_SCORE * achievementRate;

        return round(finalScore);
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
            return Math.max(totalMonthlyAvailableAmount, 0.0);
        }

        double totalPensionAndIrpAnnualPayment =
                memberA.getPensionAnnualPayment().doubleValue()
                        + memberA.getIrpAnnualPayment().doubleValue()
                        + memberB.getPensionAnnualPayment().doubleValue()
                        + memberB.getIrpAnnualPayment().doubleValue();

        double pensionAndIrpMonthlyPayment =
                totalPensionAndIrpAnnualPayment / MONTHS_IN_YEAR;

        return Math.max(
                totalMonthlyAvailableAmount - pensionAndIrpMonthlyPayment,
                0.0
        );
    }

    private void validateGoalInput(MatchCalculationInput input) {
        if (input == null
                || input.getMemberA() == null
                || input.getMemberB() == null) {
            throw new IllegalArgumentException("두 회원의 목표 계산 입력값이 필요합니다.");
        }

        if (input.getFirstGoalType() == null) {
            throw new IllegalArgumentException("목표 유형이 필요합니다.");
        }

        if (input.getTargetAmount() == null
                || input.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("목표금액은 0보다 커야 합니다.");
        }

        if (input.getTargetPeriodMonths() <= 0) {
            throw new IllegalArgumentException("목표기간은 1개월 이상이어야 합니다.");
        }

        validateGoalMemberInput(input.getMemberA());
        validateGoalMemberInput(input.getMemberB());
    }

    private void validateGoalMemberInput(MemberCalculationInput member) {
        if (member.getFinancialAsset() == null
                || member.getMonthlyAvailableAmount() == null
                || member.getPensionSavingBalance() == null
                || member.getIrpBalance() == null
                || member.getPensionAnnualPayment() == null
                || member.getIrpAnnualPayment() == null) {
            throw new IllegalArgumentException("회원의 목표 계산 입력값이 필요합니다.");
        }
    }

    //5. 절세 활용도
    public BigDecimal calculateTaxStrategyScore(MatchCalculationInput input) {
        if (input == null
                || input.getMemberA() == null
                || input.getMemberB() == null) {
            throw new IllegalArgumentException("두 회원의 절세 계산 입력값이 필요합니다.");
        }

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

            if (isEligible(member.getTaxEligibilityStatus(), "연금")) {
                pensionUtilizationSum += calculatePensionUtilization(member);
                pensionEligibleCount++;
            }

            if (isEligible(member.getIsaEligibilityStatus(), "ISA")) {
                isaUtilizationSum += calculateIsaUtilization(member);
                isaEligibleCount++;
            }
        }

        boolean pensionApplicable = pensionEligibleCount > 0;
        boolean isaApplicable = isaEligibleCount > 0;

        double pensionScore = 0.0;

        if (pensionApplicable) {
            double couplePensionUtilization =
                    pensionUtilizationSum / pensionEligibleCount;

            pensionScore =
                    couplePensionUtilization * PENSION_MAX_SCORE;
        }

        double isaScore = 0.0;

        if (isaApplicable) {
            double coupleIsaUtilization =
                    isaUtilizationSum / isaEligibleCount;

            isaScore =
                    coupleIsaUtilization * ISA_MAX_SCORE;
        }

        double applicableMaxScore =
                (pensionApplicable ? PENSION_MAX_SCORE : 0.0)
                        + (isaApplicable ? ISA_MAX_SCORE : 0.0);

        // 두 영역 모두 평가 대상이 아니면 절세 항목은 미산출
        if (applicableMaxScore == 0.0) {
            return BigDecimal.ZERO.setScale(2);
        }

        double finalScore =
                TAX_MAX_SCORE
                        * ((pensionScore + isaScore) / applicableMaxScore);

        return round(finalScore);
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

        double recognizedPensionSaving =
                Math.min(pensionSavingPayment, PENSION_SAVING_LIMIT);

        double recognizedTotalPayment = Math.min(
                recognizedPensionSaving + irpPayment + dcPayment,
                PENSION_TOTAL_LIMIT
        );

        return recognizedTotalPayment / PENSION_TOTAL_LIMIT;
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
                fieldName + " 평가 대상 여부를 확인할 수 없습니다."
        );
    }

    private boolean isTaxStrategyCalculated(
            MatchCalculationInput input
    ) {
        MemberCalculationInput memberA = input.getMemberA();
        MemberCalculationInput memberB = input.getMemberB();

        return ELIGIBLE.equals(memberA.getTaxEligibilityStatus())
                || ELIGIBLE.equals(memberB.getTaxEligibilityStatus())
                || ELIGIBLE.equals(memberA.getIsaEligibilityStatus())
                || ELIGIBLE.equals(memberB.getIsaEligibilityStatus());
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
            throw new IllegalArgumentException("회원의 절세 계산 입력값이 필요합니다.");
        }
        if (member.getPensionAnnualPayment().compareTo(BigDecimal.ZERO) < 0
                || member.getIrpAnnualPayment().compareTo(BigDecimal.ZERO) < 0
                || member.getDcAnnualPayment().compareTo(BigDecimal.ZERO) < 0
                || member.getIsaAnnualDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "절세 납입액은 0보다 작을 수 없습니다."
            );
        }
    }

    //총점 계산
    public MatchCalculationResult calculate(MatchCalculationInput input) {
        if (input == null
                || input.getMemberA() == null
                || input.getMemberB() == null) {
            throw new IllegalArgumentException("두 회원의 궁합 계산 입력값이 필요합니다.");
        }

        BigDecimal assetStabilityScore =
                calculateAssetStabilityScore(input);

        BigDecimal debtRepaymentScore =
                calculateDebtRepaymentScore(input);

        BigDecimal financialValueScore =
                calculateFinancialValueScore(input);

        BigDecimal goalFeasibilityScore =
                calculateGoalFeasibilityScore(input);

        BigDecimal taxStrategyScore =
                calculateTaxStrategyScore(input);

        boolean taxStrategyCalculated =
                isTaxStrategyCalculated(input);

        BigDecimal scoreSum =
                assetStabilityScore
                        .add(debtRepaymentScore)
                        .add(financialValueScore)
                        .add(goalFeasibilityScore)
                        .add(taxStrategyScore);

        BigDecimal calculatedMaxScore =
                taxStrategyCalculated
                        ? new BigDecimal("100")
                        : new BigDecimal("90");

        BigDecimal totalScore =
                scoreSum
                        .multiply(new BigDecimal("100"))
                        .divide(
                                calculatedMaxScore,
                                2,
                                RoundingMode.HALF_UP
                        );

        return MatchCalculationResult.builder()
                .assetStabilityScore(assetStabilityScore)
                .debtRepaymentScore(debtRepaymentScore)
                .financialValueScore(financialValueScore)
                .goalFeasibilityScore(goalFeasibilityScore)
                .taxStrategyScore(taxStrategyScore)
                .taxStrategyCalculated(taxStrategyCalculated)
                .totalScore(totalScore)
                .build();
    }
}