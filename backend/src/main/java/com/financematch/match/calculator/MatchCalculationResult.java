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

    // 목표기간 후 예상 자산. report 도메인 reason 문구 생성용 (점수 계산과 무관, 표시 전용).
    private BigDecimal expectedAsset;

    private boolean taxStrategyCalculated;

    private BigDecimal totalScore;           //100점 만점임.
}