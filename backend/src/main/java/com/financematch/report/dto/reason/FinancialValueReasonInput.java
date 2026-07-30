package com.financematch.report.dto.reason;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 투자 가치관 일치도 축 이유 문구 생성 입력값. survey 도메인(설문 4문항 응답)이 계산해서 넘겨줄 예정.
 *
 * <p>4문항: 총자산 중 금융자산 비중 · 투자 경험 · 금융 투자 상품 이해도 · 손실 감내력.
 * 이 축은 개인 이름을 절대 쓰지 않으므로(R4) 이름 필드는 없다.
 */
@Getter
@AllArgsConstructor
public class FinancialValueReasonInput {

    private final int meAssetRatio;
    private final int meInvestExperience;
    private final int meProductUnderstanding;
    private final int meLossTolerance;

    private final int partnerAssetRatio;
    private final int partnerInvestExperience;
    private final int partnerProductUnderstanding;
    private final int partnerLossTolerance;
}
