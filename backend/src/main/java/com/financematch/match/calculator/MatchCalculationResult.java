package com.financematch.match.calculator;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class MatchCalculationResult {

    private BigDecimal assetStabilityScore;  //25점
    private BigDecimal debtRepaymentScore;   //20점
    private BigDecimal financialValueScore;  //25점
    private BigDecimal goalFeasibilityScore; //20점
    private BigDecimal taxStrategyScore;     //10점

    // 목표기간 후 예상 자산. report 도메인 reason 문구 생성용 (점수 계산과 무관, 표시 전용).
    private BigDecimal expectedAsset;

    // (A+B 금융자산)/(A+B 동연령대 중앙값). report 도메인 금융 자산 축 reason 문구 생성용.
    private Double coupleAssetRatio;

    // 회원별 부채 점수(0~100). report 도메인 부채 축 reason 문구 생성용(누가 감점에 더 영향을 줬는지 비교).
    private BigDecimal memberADebtScore;
    private BigDecimal memberBDebtScore;

    private boolean taxStrategyCalculated;

    private BigDecimal totalScore;           //100점 만점임.
}