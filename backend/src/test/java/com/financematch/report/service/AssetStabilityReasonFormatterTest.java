package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AssetStabilityReasonFormatterTest {

    private final AssetStabilityReasonFormatter formatter = new AssetStabilityReasonFormatter();

    @Test
    void ratio가_1보다_크면_안정적이라는_문구를_반환한다() {
        assertEquals(
                "두 분의 연령대를 각각 고려했을 때, 현재 합산 금융자산은 또래 기준보다 안정적인 편이에요.",
                formatter.format(1.2));
    }

    @Test
    void ratio가_정확히_1이면_낮은_쪽_문구를_반환한다() {
        assertEquals(
                "금융 자산을 쌓기 위해 더 분발할 필요가 있어요. 금융 자산을 쌓는 걸 도와줄 상품들을 추천탭에서 만나보세요.",
                formatter.format(1.0));
    }

    @Test
    void ratio가_1보다_작으면_분발_문구를_반환한다() {
        assertEquals(
                "금융 자산을 쌓기 위해 더 분발할 필요가 있어요. 금융 자산을 쌓는 걸 도와줄 상품들을 추천탭에서 만나보세요.",
                formatter.format(0.4));
    }
}
