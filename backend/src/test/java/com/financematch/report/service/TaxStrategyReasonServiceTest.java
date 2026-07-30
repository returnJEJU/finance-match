package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.report.dto.reason.TaxAccountInput;
import com.financematch.report.dto.reason.TaxSavingProfile;
import com.financematch.report.dto.reason.TaxStrategyReasonInput;
import com.financematch.report.llm.ReasonRuleValidator;
import com.financematch.report.llm.ReportLlmClient;
import com.financematch.report.llm.ReportPromptBuilder;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TaxStrategyReasonServiceTest {

    private final TaxStrategyReasonService service =
            new TaxStrategyReasonService(
                    (ReportLlmClient) null, (ReportPromptBuilder) null, (ReasonRuleValidator) null);

    private static final TaxAccountInput OPEN_FULL =
            new TaxAccountInput(true, new BigDecimal(20_000_000), new BigDecimal(20_000_000), new BigDecimal(2_000_000));

    @Test
    void 한_명만_계좌_한개_미개설이면_그_사람만_언급하고_추천탭을_안내한다() {
        // TAX-01: partner ISA 미개설, 나머지 정상
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(TaxAccountInput.unopened(), OPEN_FULL, OPEN_FULL);
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.fallback(input);

        assertEquals("이영희님은 ISA 계좌를 개설하지 않았어요. 추천탭에서 상품들을 만나보세요.", reason);
        assertTrue(!reason.contains("김철수"));
    }

    @Test
    void 한도_미달이면_한도와_혜택_금액을_명시한다() {
        // TAX-02: me ISA 한도 미달(1200만원 납입/2000만원 한도), 전부 개설
        TaxAccountInput underLimitIsa =
                new TaxAccountInput(true, new BigDecimal(12_000_000), new BigDecimal(20_000_000), new BigDecimal(2_000_000));
        TaxSavingProfile me = new TaxSavingProfile(underLimitIsa, OPEN_FULL, OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.fallback(input);

        assertEquals("김철수님은 ISA 한도 2000만원 중 200만원 혜택 가능 더 채우면 혜택을 더 받을 수 있어요.", reason);
        assertTrue(!reason.contains("이영희"));
        assertTrue(!reason.contains("추천탭")); // 규칙 3번: 한도 미달만 있을 땐 추천탭 언급 불필요
    }

    @Test
    void 세_계좌_다_미개설이면_뭉뚱그려서_말한다() {
        // GAP-07 확정 케이스: TAX-04
        TaxSavingProfile me =
                new TaxSavingProfile(
                        TaxAccountInput.unopened(), TaxAccountInput.unopened(), TaxAccountInput.unopened());
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.fallback(input);

        assertEquals("김철수님은 ISA·IRP·연금저축 모두 개설 안 하셨어요. 추천탭에서 상품들을 만나보세요.", reason);
    }

    @Test
    void 둘_다_문제있으면_가나다순으로_둘_다_언급한다() {
        // TAX-03: me(김철수) IRP 미개설, partner(이영희) pension 미개설
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, TaxAccountInput.unopened(), OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, TaxAccountInput.unopened());
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.fallback(input);

        assertTrue(reason.indexOf("김철수") < reason.indexOf("이영희"), "가나다순(김철수 먼저)이어야 함: " + reason);
    }

    @Test
    void 이름이_반전되어도_등장_순서는_가나다순으로_동일하다() {
        // TAX-06: TAX-03의 me/partner 반전. 결과 문장은 TAX-03과 동일해야 한다(뷰어 독립성).
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, TaxAccountInput.unopened());
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, TaxAccountInput.unopened(), OPEN_FULL);
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("이영희", "김철수", me, partner);

        String reason = service.fallback(input);

        assertTrue(reason.indexOf("김철수") < reason.indexOf("이영희"), "뷰어와 무관하게 가나다순이어야 함: " + reason);
    }

    @Test
    void 둘_다_전부_개설_한도충족이면_칭찬_문구를_반환한다() {
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.fallback(input);

        assertTrue(reason.contains("ISA") && reason.contains("IRP") && reason.contains("연금저축"));
        assertTrue(!reason.contains("김철수") && !reason.contains("이영희"));
    }
}
