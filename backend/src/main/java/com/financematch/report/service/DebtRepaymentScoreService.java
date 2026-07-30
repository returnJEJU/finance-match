package com.financematch.report.service;

import com.financematch.report.dto.reason.DebtRepaymentReasonInput;
import com.financematch.report.mapper.ReportWriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 부채 축 reason 을 생성·저장한다. {@link GoalFeasibilityScoreService}/{@link
 * AssetStabilityScoreService}와 같은 자리 — {@code match.calculator.MatchCalculator}가 계산을
 * 마친 직후 1회 호출된다.
 *
 * <p>이 축은 LLM을 쓰므로({@link DebtRepaymentReasonService}) 목표·자산 축과 달리 네트워크 호출이
 * 있다 — DB 트랜잭션을 오래 붙잡지 않도록 호출 위치에 유의해야 한다.
 */
@Service
@RequiredArgsConstructor
public class DebtRepaymentScoreService {

    private final DebtRepaymentReasonService reasonService;
    private final ReportWriteMapper reportWriteMapper;

    public void generateAndSave(Long compatibilityResultId, DebtRepaymentReasonInput input) {
        String reason = reasonService.generate(input);
        reportWriteMapper.upsertDebtRepaymentReason(compatibilityResultId, reason);
    }
}
