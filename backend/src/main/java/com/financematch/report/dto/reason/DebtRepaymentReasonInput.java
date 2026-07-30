package com.financematch.report.dto.reason;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 부채 축 이유 문구 생성 입력값. survey·calculator 도메인이 계산해서 넘겨줄 예정.
 *
 * <p>riskLevel 은 숫자가 클수록 위험하다는 것만 정해져 있고, 실제 등급 체계(몇 단계인지)는
 * calculator 도메인 확정 후 맞춘다. 부채가 없으면 null.
 */
@Getter
@AllArgsConstructor
public class DebtRepaymentReasonInput {

    private final String meName;
    private final String partnerName;

    private final boolean meHasDebt;
    private final boolean partnerHasDebt;

    private final Integer meRiskLevel;
    private final Integer partnerRiskLevel;
}
