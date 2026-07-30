package com.financematch.report.mapper;

import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportWriteMapper {

    /**
     * {@code report} 행이 없으면 새로 만들고, 있으면 goal_feasibility_reason 만 갱신한다
     * (compatibility_result_id 가 UNIQUE 라 ON DUPLICATE KEY UPDATE 로 upsert).
     */
    void upsertGoalFeasibilityReason(
            @Param("compatibilityResultId") Long compatibilityResultId, @Param("reason") String reason);


    /** {@code report} 행이 없으면 새로 만들고, 있으면 asset_stability_reason 만 갱신한다. */
    void upsertAssetStabilityReason(
            @Param("compatibilityResultId") Long compatibilityResultId, @Param("reason") String reason);

    /** {@code report} 행이 없으면 새로 만들고, 있으면 debt_repayment_reason 만 갱신한다. */
    void upsertDebtRepaymentReason(
            @Param("compatibilityResultId") Long compatibilityResultId, @Param("reason") String reason);
}
