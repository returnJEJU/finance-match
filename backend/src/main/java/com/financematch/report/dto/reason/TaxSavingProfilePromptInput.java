package com.financematch.report.dto.reason;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** {@link TaxSavingProfile}을 LLM 프롬프트용으로 가공한 뷰(계좌별 금액이 포맷된 문자열). */
@Getter
@AllArgsConstructor
public class TaxSavingProfilePromptInput {

    private final TaxAccountPromptInput isa;
    private final TaxAccountPromptInput irp;
    private final TaxAccountPromptInput pension;
}
