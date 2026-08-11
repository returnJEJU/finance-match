package com.financematch.match.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class FinancialValueEngine {

    /**
     * 두 회원의 투자 가치관 일치도 점수를 계산한다.
     * 최대 점수는 25점이다.
     */
    public BigDecimal calculateScore(MatchCalculationInput input) {
        validateInput(input);

        MemberCalculationInput memberA = input.getMemberA();
        MemberCalculationInput memberB = input.getMemberB();

        validateFinancialValueScores(memberA);
        validateFinancialValueScores(memberB);

        double financialAssetRatioScore =
                calculateSimilarityScore(
                        memberA.getFinancialAssetRatioScore(),
                        memberB.getFinancialAssetRatioScore(),
                        4.0,
                        5.0
                );

        double investmentExperienceScore =
                calculateSimilarityScore(
                        memberA.getInvestmentExperienceScore(),
                        memberB.getInvestmentExperienceScore(),
                        4.0,
                        4.0
                );

        double financialKnowledgeScore =
                calculateSimilarityScore(
                        memberA.getFinancialKnowledgeScore(),
                        memberB.getFinancialKnowledgeScore(),
                        4.0,
                        4.0
                );

        double capitalPreservationScore =
                calculateSimilarityScore(
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
        int difference =
                Math.abs(memberAScore - memberBScore);

        double similarity =
                1.0 - (difference / maxDifference);

        return maxScore * similarity;
    }

    private void validateFinancialValueScores(
            MemberCalculationInput member
    ) {
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

    private void validateInput(MatchCalculationInput input) {
        if (input == null
                || input.getMemberA() == null
                || input.getMemberB() == null) {
            throw new IllegalArgumentException(
                    "두 회원의 투자 가치관 계산 입력값이 필요합니다."
            );
        }
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}