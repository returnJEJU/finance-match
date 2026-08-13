package com.financematch.report.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportWriteMapper {

    /**
     * {@code report} 행이 없으면 새로 만들고, 있으면 goal_feasibility_reason·expected_asset 을 갱신한다
     * (compatibility_result_id 가 UNIQUE 라 ON DUPLICATE KEY UPDATE 로 upsert).
     *
     * <p>expected_asset 은 목표 달성 가능성 리포트 문구를 만들 때 쓴 값 그대로다 — 리포트 화면의
     * 목표 카드(부족/초과 금액·달성률·예상 가용자산)가 다시 계산하지 않고 그대로 읽어 쓴다.
     */
    void upsertGoalFeasibilityReason(
            @Param("compatibilityResultId") Long compatibilityResultId,
            @Param("reason") String reason,
            @Param("expectedAsset") BigDecimal expectedAsset);


    /** {@code report} 행이 없으면 새로 만들고, 있으면 asset_stability_reason 만 갱신한다. */
    void upsertAssetStabilityReason(
            @Param("compatibilityResultId") Long compatibilityResultId, @Param("reason") String reason);

    /** {@code report} 행이 없으면 새로 만들고, 있으면 debt_repayment_reason 만 갱신한다. */
    void upsertDebtRepaymentReason(
            @Param("compatibilityResultId") Long compatibilityResultId, @Param("reason") String reason);

    /** {@code report} 행이 없으면 새로 만들고, 있으면 financial_value_reason 만 갱신한다. */
    void upsertFinancialValueReason(
            @Param("compatibilityResultId") Long compatibilityResultId, @Param("reason") String reason);

    /** {@code report} 행이 없으면 새로 만들고, 있으면 tax_strategy_reason 만 갱신한다. */
    void upsertTaxStrategyReason(
            @Param("compatibilityResultId") Long compatibilityResultId, @Param("reason") String reason);

    /** {@code report} 행이 없으면 새로 만들고, 있으면 expert_comment(종합 코멘트) 만 갱신한다. */
    void upsertExpertComment(
            @Param("compatibilityResultId") Long compatibilityResultId, @Param("comment") String comment);
}
