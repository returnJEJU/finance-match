package com.financematch.report.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 목표 달성 가능성 리포트 문구 생성기 (LLM 미사용).
 *
 * <p>expectedAsset·targetAmount·monthsUntilGoal 은 survey·calculator 도메인이 계산해서 넘겨줄
 * 예정이다. 두 도메인이 아직 없어, 이 클래스는 그 결과값을 받아 문장만 완성한다 — DB 접근·계산 없음.
 * 도메인이 준비되면 이 값들을 실제로 계산해서 넘기기만 하면 된다.
 */
@Component
public class GoalFeasibilityReasonFormatter {

    public String format(BigDecimal expectedAsset, BigDecimal targetAmount, int monthsUntilGoal) {
        if (expectedAsset.compareTo(targetAmount) < 0) {
            BigDecimal shortfall = targetAmount.subtract(expectedAsset);
            return String.format(
                    "%d개월 후 예상 자산은 %s이에요. 목표 금액 %s 대비 %s 모자라요. "
                            + "목표 달성을 도와줄 상품들이 추천탭에 준비되어있어요.",
                    monthsUntilGoal,
                    formatWon(expectedAsset),
                    formatWon(targetAmount),
                    formatWon(shortfall));
        }

        return String.format(
                "%d개월 후 목표 달성 가능성이 커요. 목표 달성을 더 확실하게 도와줄 상품을 추천탭에서 만나보세요.",
                monthsUntilGoal);
    }

    // 원 단위 금액을 "1억 2000만원" · "8000만원" · "1억" 형태로 변환한다.
    // 만원 미만 단수는 버린다 — 이 리포트 문구는 만원 단위로만 말한다.
    private String formatWon(BigDecimal won) {
        long value = won.longValueExact();
        long eok = value / 100_000_000L;
        long man = (value % 100_000_000L) / 10_000L;

        if (eok > 0 && man > 0) {
            return eok + "억 " + man + "만원";
        }
        if (eok > 0) {
            return eok + "억";
        }
        return man + "만원";
    }
}
