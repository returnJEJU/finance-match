package com.financematch.match.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class FinancialAssetEngine {

    private static final double MEMBER_WEIGHT = 0.3;
    private static final double COUPLE_WEIGHT = 0.4;
    private static final double ASSET_MAX_SCORE = 25.0;

    /**
     * 금융자산 안정성 점수를 계산한다.
     * 최대 점수는 25점이다.
     */
    public BigDecimal calculateScore(MatchCalculationInput input) {
        validateInput(input);

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

        double finalScore =
                weightedScore * (ASSET_MAX_SCORE / 100.0);

        return round(finalScore);
    }

    /**
     * 두 회원의 합산 금융자산을 합산 동연령대 자산 중앙값으로 나눈 비율이다.
     * report 영역의 금융자산 사유 문구에서도 사용한다.
     */
    public double calculateCoupleAssetRatio(
            MatchCalculationInput input
    ) {
        validateInput(input);

        MemberCalculationInput memberA = input.getMemberA();
        MemberCalculationInput memberB = input.getMemberB();

        BigDecimal coupleFinancialAsset = memberA.getFinancialAsset()
                .add(memberB.getFinancialAsset());

        BigDecimal coupleAssetMedian = memberA.getAgeGroupAssetMedian()
                .add(memberB.getAgeGroupAssetMedian());

        return calculateAssetRatio(
                coupleFinancialAsset,
                coupleAssetMedian
        );
    }

    private double calculateRelativeAssetScore(
            BigDecimal financialAsset,
            BigDecimal medianFinancialAsset
    ) {
        double ratio = calculateAssetRatio(
                financialAsset,
                medianFinancialAsset
        );

        return 100.0 * (1.0 - Math.pow(2.0, -ratio));
    }

    private double calculateAssetRatio(
            BigDecimal financialAsset,
            BigDecimal medianFinancialAsset
    ) {
        if (financialAsset == null) {
            throw new IllegalArgumentException(
                    "금융자산이 필요합니다."
            );
        }

        if (medianFinancialAsset == null
                || medianFinancialAsset.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "금융자산 중앙값은 0보다 커야 합니다."
            );
        }

        double asset = Math.max(
                financialAsset.doubleValue(),
                0.0
        );

        double median = medianFinancialAsset.doubleValue();

        return asset / median;
    }

    private void validateInput(MatchCalculationInput input) {
        if (input == null
                || input.getMemberA() == null
                || input.getMemberB() == null) {
            throw new IllegalArgumentException(
                    "두 회원의 금융자산 계산 입력값이 필요합니다."
            );
        }
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}