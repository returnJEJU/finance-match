package com.financematch.match.domain;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompatibilityResult {

    private Long id;
    private Long coupleId;

    private BigDecimal assetStabilityScore;
    private BigDecimal debtRepaymentScore;
    private BigDecimal financialValueScore;
    private BigDecimal goalFeasibilityScore;
    private BigDecimal taxStrategyScore;

    private boolean taxStrategyCalculated;

    private BigDecimal totalScore;
    private String resultSummary;
}