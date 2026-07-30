package com.financematch.report.service;

import com.financematch.report.dto.reason.FinancialValueReasonInput;
import com.financematch.report.llm.ReasonRuleValidator;
import com.financematch.report.llm.ReportLlmClient;
import com.financematch.report.llm.ReportPromptBuilder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 투자 가치관 일치도 축 reason 문구를 LLM으로 생성한다.
 *
 * <p>4문항 diff·임계값 비교는 결정론적이라 이 클래스가 직접 계산해 정답(branch)을 미리 알고, LLM
 * 출력이 그 결정과 어긋나면 재시도 후 {@link #fallback}으로 대체한다.
 *
 * <p>⚠️ 임계값 {@value #THRESHOLD}는 AGENTIC.md GAP-03 미확정 상태의 권장값(T=1)이다. 팀 확정 후
 * 이 상수만 바꾸면 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialValueReasonService {

    private static final int MAX_ATTEMPTS = 3;
    private static final int THRESHOLD = 1;

    // GAP-04 확정: 동점일 때 이 순서에서 먼저 오는 항목을 택한다.
    private static final List<String> ITEM_ORDER =
            List.of("총자산 중 금융자산 비중", "투자 경험", "금융 투자 상품 이해도", "손실 감내력");

    private final ReportLlmClient llmClient;
    private final ReportPromptBuilder promptBuilder;
    private final ReasonRuleValidator ruleValidator;

    public String generate(FinancialValueReasonInput input) {
        String prompt = promptBuilder.buildFinancialValuePrompt(input, THRESHOLD);
        String expected = fallback(input);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String candidate = llmClient.generateReason(prompt);
            if (isValid(candidate, expected)) {
                return candidate;
            }
            log.warn("가치관 축 reason 검증 실패(시도 {}/{}): {}", attempt, MAX_ATTEMPTS, candidate);
        }

        log.warn("가치관 축 reason LLM 생성 {}회 모두 실패 — 결정론적 문구로 폴백", MAX_ATTEMPTS);
        return expected;
    }

    private boolean isValid(String reason, String expectedBranchReason) {
        if (!ruleValidator.validateCommon(reason).valid()) {
            return false;
        }
        // R4: 개인 이름 절대 금지. "님"이 붙는 표현이 하나라도 있으면 이름을 부른 것으로 간주한다.
        if (reason.contains("님")) {
            return false;
        }
        // 결정된 branch 문구와 완전히 같은 문장을 요구하진 않지만(LLM 표현 다양성 허용),
        // 비교 분기라면 최소·최대 항목명이 실제로 등장하는지는 확인한다.
        return containsExpectedItemNames(reason, expectedBranchReason);
    }

    private boolean containsExpectedItemNames(String reason, String expectedBranchReason) {
        for (String item : ITEM_ORDER) {
            boolean expectedToMention = expectedBranchReason.contains(item);
            boolean actuallyMentions = reason.contains(item);
            if (expectedToMention && !actuallyMentions) {
                return false;
            }
        }
        return true;
    }

    String fallback(FinancialValueReasonInput input) {
        int[] diffs = {
            Math.abs(input.getMeAssetRatio() - input.getPartnerAssetRatio()),
            Math.abs(input.getMeInvestExperience() - input.getPartnerInvestExperience()),
            Math.abs(input.getMeProductUnderstanding() - input.getPartnerProductUnderstanding()),
            Math.abs(input.getMeLossTolerance() - input.getPartnerLossTolerance()),
        };

        int maxDiff = max(diffs);
        int minDiff = min(diffs);

        if (maxDiff <= THRESHOLD) {
            return "두 분은 가치관이 비슷해요.";
        }
        if (maxDiff == minDiff) {
            return "두 분은 네 가지 항목 모두에서 가치관 차이가 있어요.";
        }

        String minItem = ITEM_ORDER.get(firstIndexOf(diffs, minDiff));
        String maxItem = ITEM_ORDER.get(firstIndexOf(diffs, maxDiff));
        return "두 분은 " + minItem + "은 비슷하지만 " + maxItem + "이 차이가 나요.";
    }

    private static int max(int[] values) {
        int max = values[0];
        for (int value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private static int min(int[] values) {
        int min = values[0];
        for (int value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private static int firstIndexOf(int[] values, int target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) {
                return i;
            }
        }
        throw new IllegalStateException("배열에 없는 값: " + target);
    }
}
