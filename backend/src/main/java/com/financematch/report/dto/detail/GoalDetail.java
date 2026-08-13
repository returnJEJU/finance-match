package com.financematch.report.dto.detail;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoalDetail {

    private final String shortageLabel;
    private final BigDecimal shortageValue;
    private final String shortageBadge;
    private final String availableAsset;
    private final String achievementRate;
    private final BigDecimal progress;
    private final String monthlySaving;
    private final BigDecimal minMonthlySaving;
    private final BigDecimal maxMonthlySaving;
    private final BigDecimal selectedMonthlySaving;
    private final BigDecimal baseAchievement;
    private final BigDecimal maxAchievement;
    private final BigDecimal baseShortage;
    private final BigDecimal minShortage;
    private final String targetAmount;
}
