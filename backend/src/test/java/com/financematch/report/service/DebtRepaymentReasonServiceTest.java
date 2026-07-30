package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.financematch.report.dto.reason.DebtRepaymentReasonInput;
import com.financematch.report.llm.ReasonRuleValidator;
import com.financematch.report.llm.ReportLlmClient;
import com.financematch.report.llm.ReportPromptBuilder;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DebtRepaymentReasonServiceTest {

    // fallback() 은 LLM 을 안 타는 결정론적 로직이라, 협력 객체는 null 로 둬도 테스트에 영향 없다.
    private final DebtRepaymentReasonService service =
            new DebtRepaymentReasonService(
                    (ReportLlmClient) null, (ReportPromptBuilder) null, (ReasonRuleValidator) null);

    @Test
    void 둘_다_부채가_없으면_이름_없는_문구를_반환한다() {
        DebtRepaymentReasonInput input =
                new DebtRepaymentReasonInput(
                        "김철수",
                        "이영희",
                        false,
                        false,
                        new BigDecimal("100"),
                        new BigDecimal("100"));

        assertEquals("두 분 모두 부채가 없어 높은 점수가 나왔어요.", service.fallback(input));
    }

    @Test
    void 한_명만_부채있으면_그_사람을_지목한다() {
        // 부채 없는 쪽은 score=100 이 되므로, 부채 있는 쪽(score 낮음)이 자동으로 지목된다.
        DebtRepaymentReasonInput input =
                new DebtRepaymentReasonInput(
                        "김철수",
                        "이영희",
                        true,
                        false,
                        new BigDecimal("62.50"),
                        new BigDecimal("100"));

        assertEquals("부채 점수 감점에 철수님이 더 큰 영향을 끼쳤어요.", service.fallback(input));
    }

    @Test
    void 상대만_부채있으면_상대를_축약형으로_지목한다() {
        DebtRepaymentReasonInput input =
                new DebtRepaymentReasonInput(
                        "김철수",
                        "이영희",
                        false,
                        true,
                        new BigDecimal("100"),
                        new BigDecimal("40.00"));

        assertEquals("부채 점수 감점에 영희님이 더 큰 영향을 끼쳤어요.", service.fallback(input));
    }

    @Test
    void 둘_다_부채있고_score가_다르면_더_낮은_쪽을_위험하다고_지목한다() {
        // GAP-02 확정: 임계값 없이 score 를 직접 비교 — 1점 차이도 지목한다.
        DebtRepaymentReasonInput input =
                new DebtRepaymentReasonInput(
                        "김철수",
                        "이영희",
                        true,
                        true,
                        new BigDecimal("70.00"),
                        new BigDecimal("69.00"));

        assertEquals("두 분 모두 부채가 있어요. 하지만 부채 위험도는 영희님이 더 높아요.", service.fallback(input));
    }

    @Test
    void score가_반전되면_지목되는_이름도_반전된다() {
        DebtRepaymentReasonInput input =
                new DebtRepaymentReasonInput(
                        "김철수",
                        "이영희",
                        true,
                        true,
                        new BigDecimal("40.00"),
                        new BigDecimal("80.00"));

        assertEquals("두 분 모두 부채가 있어요. 하지만 부채 위험도는 철수님이 더 높아요.", service.fallback(input));
    }

    @Test
    void 둘_다_부채있고_score가_정확히_같으면_이름_없이_처리한다() {
        DebtRepaymentReasonInput input =
                new DebtRepaymentReasonInput(
                        "김철수",
                        "이영희",
                        true,
                        true,
                        new BigDecimal("55.00"),
                        new BigDecimal("55.00"));

        assertEquals("두 분 모두 부채 위험도가 높아요.", service.fallback(input));
    }
}
