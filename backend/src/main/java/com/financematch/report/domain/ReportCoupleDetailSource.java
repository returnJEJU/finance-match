package com.financematch.report.domain;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportCoupleDetailSource {

    private Long coupleId;
    private Long compatibilityResultId;
    private String firstGoalType;
    private BigDecimal targetAmount;
    private Integer targetPeriodMonths;
    private BigDecimal expectedAsset;
    private String expertComment;
}
