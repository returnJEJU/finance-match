package com.financematch.report.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

import com.financematch.report.dto.GoalProgress;

/**
 * 리포트 화면의 목표 달성 가능성 카드(진행 현황 바)에 쓰는 값을 계산한다. {@link
 * GoalFeasibilityReasonFormatter}와 같은 expectedAsset·targetAmount 를 받아, 문장 대신 화면 표시용
 * 필드(초과/부족 금액, 달성률, 예상 가용자산)로 조립한다는 점만 다르다.
 */
@Component
public class GoalProgressFormatter {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public GoalProgress format(BigDecimal expectedAsset, BigDecimal targetAmount) {
        BigDecimal diff = expectedAsset.subtract(targetAmount);
        boolean isAchieved = diff.compareTo(BigDecimal.ZERO) >= 0;
        BigDecimal diffAbs = diff.abs();

        String amountLabel = (isAchieved ? "+" : "-") + WonAmountFormatter.format(diffAbs);
        String barLabel = WonAmountFormatter.format(diffAbs) + (isAchieved ? " 초과" : " 부족");
        String availableAsset = WonAmountFormatter.format(expectedAsset);

        int ratePercent =
                targetAmount.compareTo(BigDecimal.ZERO) == 0
                        ? 0
                        : expectedAsset
                                .multiply(HUNDRED)
                                .divide(targetAmount, 0, RoundingMode.HALF_UP)
                                .intValue();
        String achievementRate = ratePercent + "%";

        return new GoalProgress(isAchieved, amountLabel, barLabel, availableAsset, achievementRate);
    }
}
