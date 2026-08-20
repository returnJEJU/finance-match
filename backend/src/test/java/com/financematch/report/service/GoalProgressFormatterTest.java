package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.report.dto.GoalProgress;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GoalProgressFormatterTest {

    private final GoalProgressFormatter formatter = new GoalProgressFormatter();

    @Test
    void 예상_자산이_목표보다_적으면_부족으로_표시한다() {
        GoalProgress progress =
                formatter.format(new BigDecimal(80_000_000), new BigDecimal(100_000_000));

        assertFalse(progress.isAchieved());
        assertEquals("-2,000만원", progress.getAmountLabel());
        assertEquals("2,000만원 부족", progress.getBarLabel());
        assertEquals("8,000만원", progress.getAvailableAsset());
        assertEquals("80%", progress.getAchievementRate());
    }

    @Test
    void 예상_자산이_목표보다_크면_초과로_표시한다() {
        GoalProgress progress =
                formatter.format(new BigDecimal(130_000_000), new BigDecimal(100_000_000));

        assertTrue(progress.isAchieved());
        assertEquals("+3,000만원", progress.getAmountLabel());
        assertEquals("3,000만원 초과", progress.getBarLabel());
        assertEquals("1억 3,000만원", progress.getAvailableAsset());
        assertEquals("130%", progress.getAchievementRate());
    }

    @Test
    void 예상_자산이_목표와_같으면_달성으로_표시한다() {
        GoalProgress progress =
                formatter.format(new BigDecimal(100_000_000), new BigDecimal(100_000_000));

        assertTrue(progress.isAchieved());
        assertEquals("+0만원", progress.getAmountLabel());
        assertEquals("100%", progress.getAchievementRate());
    }

    @Test
    void 달성률은_반올림해서_정수_퍼센트로_표시한다() {
        // 89,120,000 / 100,000,000 = 89.12% → 반올림해서 89%
        GoalProgress progress =
                formatter.format(new BigDecimal(89_120_000), new BigDecimal(100_000_000));

        assertEquals("89%", progress.getAchievementRate());
    }

    /**
     * 목표 금액이 0 이면 달성률을 계산할 수 없다(0 으로 나눔). 계산 대신 0% 로 내려보낸다 —
     * 목표를 아직 정하지 않은 커플의 리포트가 여기서 터지면 화면 전체가 안 뜬다.
     */
    @Test
    void 목표_금액이_0_이면_달성률은_0_퍼센트다() {
        GoalProgress progress = formatter.format(new BigDecimal(50_000_000), BigDecimal.ZERO);

        assertEquals("0%", progress.getAchievementRate());
    }
}
