package com.financematch.match.calculator;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class MatchCalculationResult {

    private BigDecimal assetStabilityScore;  //30점
    private BigDecimal debtRepaymentScore;   //20점
    private BigDecimal financialValueScore;  //25점
    private BigDecimal goalFeasibilityScore; //15점
    private BigDecimal taxStrategyScore;     //10점

    private boolean taxStrategyCalculated;

    private BigDecimal totalScore;           //100점 만점임.
}