package com.financematch.report.dto.reason;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * {@link TaxAccountInput}을 LLM 프롬프트용으로 가공한 뷰. 미개설이면 contributed·annualLimit 은
 * null(프롬프트에 금액 자체를 노출하지 않음).
 *
 * <p>금액은 원 단위 숫자({@code BigDecimal}) 대신 이미 "900만원" 형태 문자열로 바꿔서 넘긴다 — LLM이
 * 원 단위 raw 숫자를 그대로 베껴 써서 "9,000,000원" 같은 문구가 나오는 걸 막기 위함(입력 자체에 그런
 * 숫자가 없으면 베낄 수도 없다).
 */
@Getter
@AllArgsConstructor
public class TaxAccountPromptInput {

    private final boolean opened;
    private final String contributed;
    private final String annualLimit;
}
