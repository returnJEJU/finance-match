package com.financematch.report.dto.reason;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 부채 축 이유 문구 생성 입력값. {@code MatchCalculator.calculateMemberDebtScore()}가 계산해서
 * 넘겨줄 예정.
 *
 * <p>score 는 0~100 (부채 없으면 100, 부채가 많고 상환 여력이 적을수록 낮아짐). GAP-02 팀 확정:
 * "위험도 차이"는 임계값 없이 score 를 직접 비교해서, 둘 다 부채 있을 때 더 낮은 쪽을 "더 위험하다"고
 * 본다. 정확히 같으면(동점) 이름 없이 처리한다.
 */
@Getter
@AllArgsConstructor
public class DebtRepaymentReasonInput {

    private final String meName;
    private final String partnerName;

    private final boolean meHasDebt;
    private final boolean partnerHasDebt;

    private final BigDecimal meScore;
    private final BigDecimal partnerScore;
}
