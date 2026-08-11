package com.financematch.report.service;

import com.financematch.report.dto.reason.DebtRepaymentReasonInput;
import org.springframework.stereotype.Service;

/**
 * 부채 축 reason 문구를 결정론적 규칙으로 생성한다.
 *
 * <p>GAP-02 팀 확정: "위험도 차이"는 임계값 없이 {@code meScore}/{@code partnerScore}(둘 다
 * 부채있음 케이스에서)를 직접 비교한다 — 조금이라도 낮은 쪽(=더 위험한 쪽)을 지목하고, 정확히 같을
 * 때만(사실상 거의 없음) 이름 없이 처리한다.
 */
@Service
public class DebtRepaymentReasonService {

    public String generate(DebtRepaymentReasonInput input) {
        return fallback(input);
    }

    String fallback(DebtRepaymentReasonInput input) {
        boolean meHasDebt = input.isMeHasDebt();
        boolean partnerHasDebt = input.isPartnerHasDebt();

        if (!meHasDebt && !partnerHasDebt) {
            return "두 분 모두 부채가 없어 높은 점수가 나왔어요.";
        }

        int scoreCompare = input.getMeScore().compareTo(input.getPartnerScore());

        if (meHasDebt && partnerHasDebt) {
            if (scoreCompare == 0) {
                return "두 분 모두 부채 위험도가 높아요.";
            }
            String riskierName =
                    KoreanNameFormatter.abbreviate(
                            scoreCompare < 0 ? input.getMeName() : input.getPartnerName());
            return "두 분 모두 부채가 있어요. 하지만 부채 위험도는 " + riskierName + "님이 더 높아요.";
        }

        // 정확히 한 명만 부채 있음 — score 가 더 낮은 쪽(=부채 있는 쪽)을 지목.
        String affectedName =
                KoreanNameFormatter.abbreviate(
                        scoreCompare < 0 ? input.getMeName() : input.getPartnerName());
        return "부채 점수 감점에 " + affectedName + "님이 더 큰 영향을 끼쳤어요.";
    }
}
