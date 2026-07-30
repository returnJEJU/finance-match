package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.financematch.report.dto.reason.DebtRepaymentReasonInput;
import com.financematch.report.llm.ReasonRuleValidator;
import com.financematch.report.llm.ReportLlmClient;
import com.financematch.report.llm.ReportPromptBuilder;
import org.junit.jupiter.api.Test;

class DebtRepaymentReasonServiceTest {

    // fallback() 은 LLM 을 안 타는 결정론적 로직이라, 협력 객체는 null 로 둬도 테스트에 영향 없다.
    private final DebtRepaymentReasonService service =
            new DebtRepaymentReasonService(
                    (ReportLlmClient) null, (ReportPromptBuilder) null, (ReasonRuleValidator) null);

    @Test
    void 둘_다_부채가_없으면_이름_없는_문구를_반환한다() {
        DebtRepaymentReasonInput input =
                new DebtRepaymentReasonInput("김철수", "이영희", false, false, null, null);

        assertEquals("두 분 모두 부채가 없어 높은 점수가 나왔어요.", service.fallback(input));
    }

    @Test
    void 둘_다_부채있고_위험도_동일하면_이름_없는_문구를_반환한다() {
        DebtRepaymentReasonInput input =
                new DebtRepaymentReasonInput("김철수", "이영희", true, true, 4, 4);

        assertEquals("두 분 모두 부채 위험도가 높아요.", service.fallback(input));
    }

    @Test
    void 둘_다_부채있고_위험도_다르면_더_위험한_쪽_이름만_언급한다() {
        DebtRepaymentReasonInput input =
                new DebtRepaymentReasonInput("김철수", "이영희", true, true, 2, 5);

        assertEquals("두 분 모두 부채가 있어요. 하지만 부채 위험도는 이영희님이 더 높아요.", service.fallback(input));
    }

    @Test
    void 위험도_반전되면_이름도_반전된다() {
        DebtRepaymentReasonInput input =
                new DebtRepaymentReasonInput("김철수", "이영희", true, true, 5, 2);

        assertEquals("두 분 모두 부채가 있어요. 하지만 부채 위험도는 김철수님이 더 높아요.", service.fallback(input));
    }

    @Test
    void 한_명만_부채있으면_이름_없는_문구를_반환한다() {
        DebtRepaymentReasonInput meOnly =
                new DebtRepaymentReasonInput("김철수", "이영희", true, false, 3, null);
        DebtRepaymentReasonInput partnerOnly =
                new DebtRepaymentReasonInput("김철수", "이영희", false, true, null, 3);

        // 누가 부채자든 결과 문구는 동일해야 한다(뷰어 독립성과 같은 취지).
        assertEquals("두 분 중 부채가 있는 분이 있어요.", service.fallback(meOnly));
        assertEquals("두 분 중 부채가 있는 분이 있어요.", service.fallback(partnerOnly));
    }
}
