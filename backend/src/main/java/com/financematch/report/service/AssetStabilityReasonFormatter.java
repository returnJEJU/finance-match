package com.financematch.report.service;

import org.springframework.stereotype.Component;

/**
 * 금융 자산 축 리포트 문구 생성기 (LLM 미사용).
 *
 * <p>ratio = (A+B 금융자산) / (A+B 동연령대 자산 중앙값). 연령대(20대/30대 등)는 문구에 노출하지
 * 않기로 했고, 판단은 ratio 하나로만 한다 — 가변 값이 없는 고정 문장 2개뿐이라 LLM이 필요 없다.
 *
 * <p>경계값: ratio 가 정확히 1이면 "낮은 경우"로 처리한다(팀 확정).
 */
@Component
public class AssetStabilityReasonFormatter {

    public String format(double ratio) {
        if (ratio > 1.0) {
            return "두 분의 연령대를 각각 고려했을 때, 현재 합산 금융자산은 또래 기준보다 안정적인 편이에요.";
        }
        return "금융 자산을 쌓기 위해 더 분발할 필요가 있어요. 금융 자산을 쌓는 걸 도와줄 상품들을 추천탭에서 만나보세요.";
    }
}
