package com.financematch.report.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScoreAxis {

    // 프론트가 아이콘·목표카드 판별 등에 쓰는 안정적 식별자. 카피(name)가 바뀌어도 변하지 않는다.
    // ASSET_STABILITY · DEBT_REPAYMENT · FINANCIAL_VALUE · GOAL_FEASIBILITY · TAX_STRATEGY
    private final String key;
    private final String name;
    private final BigDecimal score;
    private final Integer maxScore;
    private final String reason;
}
