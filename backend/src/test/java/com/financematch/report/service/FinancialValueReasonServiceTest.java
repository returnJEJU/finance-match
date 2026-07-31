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
        // 5단계 문항 3개는 diff=4, 6단계 손실 감내력은 diff=5 (보정 후 4.0) → 보정 후 전부 동일
        FinancialValueReasonInput input = new FinancialValueReasonInput(1, 1, 1, 1, 5, 5, 5, 6);

        assertEquals("두 분은 네 가지 항목 모두에서 가치관 차이가 있어요.", service.fallback(input));
    }

    @Test
    void diff가_들쭉날쭉하면_최대_항목만_지목한다() {
        // productUnderstanding diff=3(최대) — 나머지 항목은 비슷하다고 언급하지 않는다.
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 2, 4, 3, 3, 2, 1, 2);

        assertEquals("두 분은 금융 투자 상품 이해도가 차이가 나요.", service.fallback(input));
    }

    @Test
    void 최댓값이_동점이면_고정_순서상_앞선_항목을_택한다() {
        // productUnderstanding diff=3, lossTolerance diff=|3-6|*0.8=2.4 → 둘 다 최댓값 아님, 무관
        // capitalPreservation 대신 investExperience diff=3으로 productUnderstanding과 동률을 만든다.
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 2, 4, 3, 3, 5, 1, 3);

        assertEquals("두 분은 투자 경험이 차이가 나요.", service.fallback(input));
    }

    @Test
    void 손실_감내력_diff는_6단계_척도라_4대5로_비례_보정된다() {
        // lossTolerance(1~6, 최대 diff 5) raw diff=5(최대) → 4.0으로 보정, productUnderstanding diff=4(1~5, 최대)와 동률
        // 동률이면 고정 순서상 앞선 "금융 투자 상품 이해도"가 최댓값 항목으로 선택된다.
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 3, 5, 1, 3, 3, 1, 6);

        assertEquals("두 분은 금융 투자 상품 이해도가 차이가 나요.", service.fallback(input));
    }

    @Test
    void 손실_감내력_diff가_1이면_비례_보정_후에도_임계값_이내다() {
        // lossTolerance raw diff=1 → 보정 후 0.8 (THRESHOLD=1 이내), 나머지 문항도 diff<=1
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 3, 3, 3, 4, 4, 4, 4);

        assertEquals("두 분은 가치관이 비슷해요.", service.fallback(input));
    }
}
