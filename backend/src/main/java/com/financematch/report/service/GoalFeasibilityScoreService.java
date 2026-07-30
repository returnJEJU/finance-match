package com.financematch.report.service;

import com.financematch.report.mapper.ReportWriteMapper;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 목표 달성 가능성 축 reason 을 계산·저장한다. {@code match.calculator.MatchCalculator}가 계산을
 * 마친 직후(설문 완료로 궁합 점수를 처음 계산하는 시점) 1회만 호출되는 것을 전제로 한다 — LLM을 쓰지
 * 않는 결정론적 로직이라 DB 트랜잭션 안에서 호출해도 안전하다.
 */
@Service
@RequiredArgsConstructor
public class GoalFeasibilityScoreService {

    private final GoalFeasibilityReasonFormatter formatter;
    private final ReportWriteMapper reportWriteMapper;

    public void generateAndSave(
            Long compatibilityResultId,
            BigDecimal expectedAsset,
            BigDecimal targetAmount,
            int targetPeriodMonths) {
        String reason = formatter.format(expectedAsset, targetAmount, targetPeriodMonths);
        reportWriteMapper.upsertGoalFeasibilityReason(compatibilityResultId, reason);
    }
}
