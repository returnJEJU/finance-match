package com.financematch.report.service;

import com.financematch.report.mapper.ReportWriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 금융 자산 축 reason 을 계산·저장한다. {@link GoalFeasibilityScoreService}와 같은 자리 —
 * {@code match.calculator.MatchCalculator}가 계산을 마친 직후 1회 호출된다. LLM을 쓰지 않는
 * 결정론적 로직이라 DB 트랜잭션 안에서 호출해도 안전하다.
 */
@Service
@RequiredArgsConstructor
public class AssetStabilityScoreService {

    private final AssetStabilityReasonFormatter formatter;
    private final ReportWriteMapper reportWriteMapper;

    public void generateAndSave(Long compatibilityResultId, double coupleAssetRatio) {
        String reason = formatter.format(coupleAssetRatio);
        reportWriteMapper.upsertAssetStabilityReason(compatibilityResultId, reason);
    }
}
