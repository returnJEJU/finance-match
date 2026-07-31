package com.financematch.report.service;

import com.financematch.report.dto.reason.FinancialValueReasonInput;
import com.financematch.report.mapper.ReportWriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 투자 가치관 일치도 축 reason 을 생성·저장한다. {@link DebtRepaymentScoreService}와 같은 자리 —
 * {@code match.calculator.MatchCalculator}가 계산을 마친 직후 1회 호출된다.
 *
 * <p>이 축은 LLM을 쓰므로({@link FinancialValueReasonService}) DB 트랜잭션을 오래 붙잡지 않도록
 * 호출 위치에 유의해야 한다.
 */
@Service
@RequiredArgsConstructor
public class FinancialValueScoreService {

    private final FinancialValueReasonService reasonService;
    private final ReportWriteMapper reportWriteMapper;

    public void generateAndSave(Long compatibilityResultId, FinancialValueReasonInput input) {
        String reason = reasonService.generate(input);
        reportWriteMapper.upsertFinancialValueReason(compatibilityResultId, reason);
    }
}
