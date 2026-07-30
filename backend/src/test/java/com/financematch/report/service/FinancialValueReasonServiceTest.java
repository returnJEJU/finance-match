package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.financematch.report.dto.reason.FinancialValueReasonInput;
import com.financematch.report.llm.ReasonRuleValidator;
import com.financematch.report.llm.ReportLlmClient;
import com.financematch.report.llm.ReportPromptBuilder;
import org.junit.jupiter.api.Test;

class FinancialValueReasonServiceTest {

    private final FinancialValueReasonService service =
            new FinancialValueReasonService(
                    (ReportLlmClient) null, (ReportPromptBuilder) null, (ReasonRuleValidator) null);

    @Test
    void 전_문항_diff가_임계값_이하면_비슷하다는_문구를_반환한다() {
        // 전부 diff=1, THRESHOLD=1 → 경계값에서 "비슷함" 판정
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 3, 3, 3, 4, 4, 4, 4);

        assertEquals("두 분은 가치관이 비슷해요.", service.fallback(input));
    }

    @Test
    void 전_문항_diff가_임계값_초과이고_전부_동일하면_네가지_항목_문구를_반환한다() {
        // 전부 diff=2 (THRESHOLD=1 초과) 이고 서로 같음
        FinancialValueReasonInput input = new FinancialValueReasonInput(1, 1, 1, 1, 3, 3, 3, 3);

        assertEquals("두 분은 네 가지 항목 모두에서 가치관 차이가 있어요.", service.fallback(input));
    }

    @Test
    void diff가_들쭉날쭉하면_최소_최대_항목을_지목한다() {
        // assetRatio diff=0(최소), productUnderstanding diff=3(최대)
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 2, 4, 3, 3, 2, 1, 2);

        assertEquals(
                "두 분은 총자산 중 금융자산 비중은 비슷하지만 금융 투자 상품 이해도이 차이가 나요.",
                service.fallback(input));
    }

    @Test
    void 최소값이_동점이면_고정_순서상_앞선_항목을_택한다() {
        // assetRatio diff=0, investExperience diff=0 (동점) → 고정 순서상 "총자산 중 금융자산 비중" 먼저
        // productUnderstanding diff=3 (최대)
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 2, 4, 3, 3, 2, 1, 3);

        assertEquals(
                "두 분은 총자산 중 금융자산 비중은 비슷하지만 금융 투자 상품 이해도이 차이가 나요.",
                service.fallback(input));
    }
}
