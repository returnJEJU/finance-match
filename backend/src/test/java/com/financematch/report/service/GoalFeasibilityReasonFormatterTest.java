package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GoalFeasibilityReasonFormatterTest {

    private final GoalFeasibilityReasonFormatter formatter = new GoalFeasibilityReasonFormatter();

    @Test
    void 예상_자산이_목표보다_적으면_부족_문구를_반환한다() {
        String reason =
                formatter.format(new BigDecimal(80_000_000), new BigDecimal(100_000_000), 3);

        assertEquals(

                "3개월 후 예상 자산은 약 8000만원이에요. 목표 금액 1억 대비 약 2000만원 모자라요. "

                        + "목표 달성을 도와줄 상품들이 추천탭에 준비되어있어요.",
                reason);
    }

    @Test
    void 만원_단위_숫자는_반올림_없이_그대로_표시한다() {
        // 8912만원 (89,120,000원) — 1000만 자리까지 다 보여주고 반올림하지 않는다.
        String reason = formatter.format(new BigDecimal(89_120_000), new BigDecimal(100_000_000), 3);

        assertEquals(
                "3개월 후 예상 자산은 약 8912만원이에요. 목표 금액 1억 대비 약 1088만원 모자라요. "

                        + "목표 달성을 도와줄 상품들이 추천탭에 준비되어있어요.",
                reason);
    }

    @Test
    void 만원_미만_소수점은_버린다() {
        // 10.2만원(102,000원)은 10만원으로 — 만원 미만 소수점만 버린다.
        // 차액도 89.8만원(898,000원)이 89만원으로 버려진다.
        String reason = formatter.format(new BigDecimal(102_000), new BigDecimal(1_000_000), 1);

        assertEquals(
                "1개월 후 예상 자산은 약 10만원이에요. 목표 금액 100만원 대비 약 89만원 모자라요. "
                        + "목표 달성을 도와줄 상품들이 추천탭에 준비되어있어요.",
                reason);
    }

    @Test
    void 예상_자산이_목표와_같으면_달성_문구를_반환한다() {
        String reason =
                formatter.format(new BigDecimal(100_000_000), new BigDecimal(100_000_000), 3);

        assertEquals(
                "3개월 후 목표 달성 가능성이 커요. 목표 달성을 더 확실하게 도와줄 상품을 추천탭에서 만나보세요.",
                reason);
    }

    @Test
    void 예상_자산이_목표보다_크면_달성_문구를_반환한다() {
        String reason =
                formatter.format(new BigDecimal(130_000_000), new BigDecimal(100_000_000), 3);

        assertEquals(
                "3개월 후 목표 달성 가능성이 커요. 목표 달성을 더 확실하게 도와줄 상품을 추천탭에서 만나보세요.",
                reason);
    }
}
