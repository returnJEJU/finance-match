package com.financematch.report.dto.reason;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** {@link TaxStrategyReasonInput}을 LLM 프롬프트로 보낼 때 쓰는 뷰(이름 축약형 + 금액 포맷 문자열). */
@Getter
@AllArgsConstructor
public class TaxStrategyPromptInput {

    private final String meName;
    private final String partnerName;

    private final TaxSavingProfilePromptInput me;
    private final TaxSavingProfilePromptInput partner;
}
