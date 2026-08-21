package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.financematch.report.dto.reason.FinancialValueReasonInput;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FinancialValueReasonServiceTest {

    private final FinancialValueReasonService service = new FinancialValueReasonService();

    @Test
    void 전_문항_diff가_임계값_이하면_비슷하다는_문구를_반환한다() {
        // 전부 diff=1, THRESHOLD=1 → 경계값에서 "비슷함" 판정
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 3, 3, 3, 4, 4, 4, 4);

        assertEquals("두 분은 가치관이 비슷해요.", service.generate(input));
    }

    @Test
    void 비슷한_항목이_하나도_없으면_다른_항목_전부를_나열한다() {
        // 5단계 문항 3개는 diff=4, 6단계 손실 감내력은 diff=5 (보정 후 4.0) → 넷 다 THRESHOLD 초과
        FinancialValueReasonInput input = new FinancialValueReasonInput(1, 1, 1, 1, 5, 5, 5, 6);

        assertEquals(
                "두 분은 총자산 중 금융자산 비중·투자 경험·금융 투자 상품 이해도·손실 감내력이 차이가 나요.",
                service.generate(input));
    }

    @Test
    void 비슷한_항목과_다른_항목이_섞여있으면_둘_다_나열한다() {
        // productUnderstanding diff=3(다름) — 나머지(assetRatio=0, investExperience=0, lossTolerance=0.8)는 비슷함
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 2, 4, 3, 3, 2, 1, 2);

        assertEquals(
                "두 분은 총자산 중 금융자산 비중·투자 경험·손실 감내력은 비슷하지만, 금융 투자 상품 이해도는 차이가 나요.",
                service.generate(input));
    }

    @Test
    void 다른_항목이_여러_개면_고정_순서대로_전부_나열한다() {
        // investExperience diff=3, productUnderstanding diff=3 → 둘 다 다름. assetRatio·lossTolerance는 비슷함.
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 2, 4, 3, 3, 5, 1, 3);

        assertEquals(
                "두 분은 총자산 중 금융자산 비중·손실 감내력은 비슷하지만, 투자 경험·금융 투자 상품 이해도는 차이가 나요.",
                service.generate(input));
    }

    @Test
    void 손실_감내력_diff는_6단계_척도라_4대5로_비례_보정된다() {
        // lossTolerance(1~6, 최대 diff 5) raw diff=5 → 4.0으로 보정, productUnderstanding diff=4와 둘 다 THRESHOLD 초과
        // assetRatio·investExperience는 diff=0으로 비슷함.
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 3, 5, 1, 3, 3, 1, 6);

        assertEquals(
                "두 분은 총자산 중 금융자산 비중·투자 경험은 비슷하지만, 금융 투자 상품 이해도·손실 감내력은 차이가 나요.",
                service.generate(input));
    }

    @Test
    void 손실_감내력_diff가_1이면_비례_보정_후에도_임계값_이내다() {
        // lossTolerance raw diff=1 → 보정 후 0.8 (THRESHOLD=1 이내), 나머지 문항도 diff<=1
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 3, 3, 3, 4, 4, 4, 4);

        assertEquals("두 분은 가치관이 비슷해요.", service.generate(input));
    }

    /**
     * 받침 유무로 이/가를 고르는 조사 처리.
     *
     * <p>지금은 "가" 쪽이 공개 경로로 닿지 않는다 — 이 분기를 타려면 항목 4개가 전부 달라야 하는데,
     * 그러면 이어붙인 문자열이 항상 "손실 감내력"(받침 있음)으로 끝나기 때문이다. <b>항목 이름이
     * 바뀌는 순간 살아나는 코드</b>라, 그때 "이해도이 차이가 나요" 같은 문장이 나가지 않도록
     * 메서드를 직접 호출해 양쪽을 고정한다.
     */
    @Test
    void 받침이_있으면_이_를_붙인다() {
        assertEquals(
                "투자 경험이",
                ReflectionTestUtils.invokeMethod(service, "withSubjectParticle", "투자 경험"));
    }

    @Test
    void 받침이_없으면_가_를_붙인다() {
        assertEquals(
                "금융 투자 상품 이해도가",
                ReflectionTestUtils.invokeMethod(
                        service, "withSubjectParticle", "금융 투자 상품 이해도"));
    }
}
