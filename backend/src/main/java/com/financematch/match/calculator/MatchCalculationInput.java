package com.financematch.match.calculator;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchCalculationInput {

    private MemberCalculationInput memberA;
    private MemberCalculationInput memberB;

    // 공동 목표
    private String firstGoalType;
    private BigDecimal targetAmount;
    private int targetPeriodMonths;
}