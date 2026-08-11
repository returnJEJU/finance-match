package com.financematch.report.service;

import com.financematch.report.dto.reason.FinancialValueReasonInput;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 투자 가치관 일치도 축 reason 문구를 결정론적 규칙으로 생성한다.
 *
 * <p>⚠️ 임계값 {@value #THRESHOLD}는 AGENTIC.md GAP-03 미확정 상태의 권장값(T=1)이다. 팀 확정 후
 * 이 상수만 바꾸면 된다.
 *
 * <p>4문항 중 "손실 감내력"(capitalPreservationScore)만 실제로는 1~6(최대 diff 5) 척도라, 나머지
 * 세 문항(1~5, 최대 diff 4)과 같은 기준으로 비교되도록 diff 를 4/5 로 비례 보정한다.
 */
@Service
public class FinancialValueReasonService {

    private static final int THRESHOLD = 1;

    // GAP-04 확정: 동점일 때 이 순서에서 먼저 오는 항목을 택한다.
    private static final List<String> ITEM_ORDER =
            List.of("총자산 중 금융자산 비중", "투자 경험", "금융 투자 상품 이해도", "손실 감내력");

    public String generate(FinancialValueReasonInput input) {
        return fallback(input);
    }

    String fallback(FinancialValueReasonInput input) {
        double[] diffs = {
            Math.abs(input.getMeAssetRatio() - input.getPartnerAssetRatio()),
            Math.abs(input.getMeInvestExperience() - input.getPartnerInvestExperience()),
            Math.abs(input.getMeProductUnderstanding() - input.getPartnerProductUnderstanding()),
            // 손실 감내력(capitalPreservationScore)은 1~6(최대 diff 5) 척도라, 나머지 세 문항
            // (1~5, 최대 diff 4)과 같은 기준으로 비교할 수 있도록 비례 보정한다.
            Math.abs(input.getMeLossTolerance() - input.getPartnerLossTolerance()) * (4.0 / 5.0),
        };

        // 항목별로 diff가 THRESHOLD 이하면 "비슷한 항목", 초과면 "다른 항목"으로 나눈다 — 한 항목만
        // 짚어주던 이전 방식보다 정보량이 많아, 비슷한 부분과 다른 부분을 같이 알려준다.
        List<String> similarItems = new ArrayList<>();
        List<String> differentItems = new ArrayList<>();
        for (int i = 0; i < diffs.length; i++) {
            (diffs[i] <= THRESHOLD ? similarItems : differentItems).add(ITEM_ORDER.get(i));
        }

        if (differentItems.isEmpty()) {
            return "두 분은 가치관이 비슷해요.";
        }
        if (similarItems.isEmpty()) {
            return "두 분은 " + withSubjectParticle(String.join("·", differentItems)) + " 차이가 나요.";
        }
        return "두 분은 "
                + withTopicParticle(String.join("·", similarItems))
                + " 비슷하지만, "
                + withTopicParticle(String.join("·", differentItems))
                + " 차이가 나요.";
    }

    // 받침 유무에 따라 이/가·은/는을 고른다 ("금융 투자 상품 이해도"처럼 받침 없는 항목명도 있어서 필요).
    // "·"로 여러 항목을 이어붙인 문자열도 마지막 글자(=마지막 항목의 끝 글자) 기준으로 그대로 적용된다.
    private static boolean hasBatchim(String word) {
        char last = word.charAt(word.length() - 1);
        return (last - 0xAC00) % 28 != 0;
    }

    private static String withSubjectParticle(String word) {
        return word + (hasBatchim(word) ? "이" : "가");
    }

    private static String withTopicParticle(String word) {
        return word + (hasBatchim(word) ? "은" : "는");
    }
}
