package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.report.dto.reason.TaxAccountInput;
import com.financematch.report.dto.reason.TaxSavingProfile;
import com.financematch.report.dto.reason.TaxStrategyReasonInput;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TaxStrategyReasonServiceTest {

    private final TaxStrategyReasonService service = new TaxStrategyReasonService();

    private static final TaxAccountInput OPEN_FULL =
            new TaxAccountInput(true, new BigDecimal(20_000_000), new BigDecimal(20_000_000));

    @Test
    void 한_명만_계좌_한개_미개설이면_그_사람만_언급하고_추천탭을_안내한다() {
        // TAX-01: partner ISA 미개설, 나머지 정상
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(TaxAccountInput.unopened(), OPEN_FULL, OPEN_FULL);
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.generate(input);

        // GAP-08: 성을 뗀 축약형("영희")으로 부른다.
        assertEquals("영희님은 ISA 계좌를 개설하지 않았어요. 추천탭에서 상품들을 만나보세요.", reason);
        assertTrue(!reason.contains("철수"));
    }

    @Test
    void 한도_미달이면_한도_금액을_명시하고_혜택_금액은_언급하지_않는다() {
        // TAX-02: me ISA 한도 미달(1200만원 납입/2000만원 한도), 전부 개설
        TaxAccountInput underLimitIsa =
                new TaxAccountInput(true, new BigDecimal(12_000_000), new BigDecimal(20_000_000));
        TaxSavingProfile me = new TaxSavingProfile(underLimitIsa, OPEN_FULL, OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.generate(input);

        assertEquals("철수님은 올해 ISA 한도 2,000만원을 다 채우지 않았어요. 더 채우고 세제 혜택 받으세요.", reason);
        assertTrue(!reason.contains("영희"));
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

        String reason = service.generate(input);

        assertEquals("철수님은 ISA·IRP·연금저축 모두 개설 안 하셨어요. 추천탭에서 상품들을 만나보세요.", reason);
    }

    @Test
    void 둘_다_문제있으면_가나다순으로_둘_다_언급한다() {
        // TAX-03: me(김철수) IRP 미개설, partner(이영희) pension 미개설
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, TaxAccountInput.unopened(), OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, TaxAccountInput.unopened());
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.generate(input);

        assertTrue(reason.indexOf("철수") < reason.indexOf("영희"), "가나다순(철수 먼저)이어야 함: " + reason);
    }

    @Test
    void 둘_다_미개설_계좌가_있으면_추천탭_안내는_맨_뒤에서_한_번만_한다() {
        // TAX-03과 동일 입력이지만, 이번엔 "추천탭" 문구가 정확히 한 번만 등장하고 문장 맨 뒤에 오는지 확인.
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, TaxAccountInput.unopened(), OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, TaxAccountInput.unopened());
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.generate(input);

        assertEquals(
                "철수님은 IRP 계좌를 개설하지 않았어요. 영희님은 연금저축 계좌를 개설하지 않았어요. 추천탭에서 상품들을 만나보세요.",
                reason);
        assertEquals(reason.lastIndexOf("추천탭"), reason.indexOf("추천탭"), "추천탭 언급은 한 번뿐이어야 함: " + reason);
        assertTrue(reason.endsWith("추천탭에서 상품들을 만나보세요."), "추천탭 안내는 문장 맨 뒤여야 함: " + reason);
    }

    @Test
    void 미개설_사람이_앞에_오면_추천탭_안내도_그_사람_설명_바로_뒤에_온다() {
        // 철수(가나다순 먼저)는 IRP 미개설, 영희(뒤)는 한도 미달만 있음 — 추천탭은 "철수" 설명 바로 뒤,
        // 즉 문장 맨 뒤(영희의 한도 안내 뒤)가 아니라 중간에 와야 한다.
        TaxAccountInput underLimitIsa =
                new TaxAccountInput(true, new BigDecimal(12_000_000), new BigDecimal(20_000_000));
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, TaxAccountInput.unopened(), OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(underLimitIsa, OPEN_FULL, OPEN_FULL);
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.generate(input);

        assertEquals(
                "철수님은 IRP 계좌를 개설하지 않았어요. 추천탭에서 상품들을 만나보세요. "
                        + "영희님은 올해 ISA 한도 2,000만원을 다 채우지 않았어요. 더 채우고 세제 혜택 받으세요.",
                reason);
        assertTrue(!reason.endsWith("추천탭에서 상품들을 만나보세요."), "추천탭이 맨 뒤가 아니라 철수 설명 뒤에 와야 함: " + reason);
    }

    @Test
    void 미개설_사람이_뒤에_오면_추천탭_안내는_맨_뒤에_온다() {
        // 철수(앞)는 한도 미달만 있음, 영희(뒤)는 IRP 미개설 — 추천탭은 영희 설명 뒤이자 문장 맨 뒤.
        TaxAccountInput underLimitIsa =
                new TaxAccountInput(true, new BigDecimal(12_000_000), new BigDecimal(20_000_000));
        TaxSavingProfile me = new TaxSavingProfile(underLimitIsa, OPEN_FULL, OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, TaxAccountInput.unopened(), OPEN_FULL);
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.generate(input);

        assertEquals(
                "철수님은 올해 ISA 한도 2,000만원을 다 채우지 않았어요. 더 채우고 세제 혜택 받으세요. "
                        + "영희님은 IRP 계좌를 개설하지 않았어요. 추천탭에서 상품들을 만나보세요.",
                reason);
        assertTrue(reason.endsWith("추천탭에서 상품들을 만나보세요."), "추천탭이 맨 뒤여야 함: " + reason);
    }

    @Test
    void 이름이_반전되어도_등장_순서는_가나다순으로_동일하다() {
        // TAX-06: TAX-03의 me/partner 반전. 결과 문장은 TAX-03과 동일해야 한다(뷰어 독립성).
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, TaxAccountInput.unopened());
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, TaxAccountInput.unopened(), OPEN_FULL);
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("이영희", "김철수", me, partner);

        String reason = service.generate(input);

        assertTrue(reason.indexOf("철수") < reason.indexOf("영희"), "뷰어와 무관하게 가나다순이어야 함: " + reason);
    }

    @Test
    void 둘_다_전부_개설_한도충족이면_칭찬_문구를_반환한다() {
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", me, partner);

        String reason = service.generate(input);

        assertTrue(reason.contains("ISA") && reason.contains("IRP") && reason.contains("연금저축"));
        assertTrue(!reason.contains("철수") && !reason.contains("영희"));
    }
}
