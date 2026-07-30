package com.financematch.report.dto.reason;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 절세 축 이유 문구 생성 입력값. survey·calculator 도메인이 계산해서 넘겨줄 예정.
 *
 * <p>이 축은 대상자 이름을 반드시 언급해야 하므로(R4) 이름 필드가 있다.
 */
@Getter
@AllArgsConstructor
public class TaxStrategyReasonInput {

    private final String meName;
    private final String partnerName;

    private final TaxSavingProfile me;
    private final TaxSavingProfile partner;
}
