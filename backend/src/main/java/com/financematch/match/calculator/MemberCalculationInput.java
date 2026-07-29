package com.financematch.match.calculator;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberCalculationInput {

    // 금융자산 계산
    private int age;
    private BigDecimal ageGroupAssetMedian;
    private BigDecimal financialAsset;

    // 부채 계산
    private BigDecimal annualIncome;
    private BigDecimal totalDebt;
    private BigDecimal annualDebtPayment;

    // 금융 가치관 일치도 계산
    private int financialAssetRatioScore;
    private int investmentExperienceScore;
    private int financialKnowledgeScore;
    private int capitalPreservationScore;

    // 목표 달성 가능성 계산
    private BigDecimal monthlyAvailableAmount;
    private BigDecimal pensionSavingBalance;
    private BigDecimal irpBalance;
    private BigDecimal pensionAnnualPayment;
    private BigDecimal irpAnnualPayment;

    // 절세 활용도 계산
    private BigDecimal dcAnnualPayment;
    private BigDecimal isaAnnualDeposit;
    private String taxEligibilityStatus;
    private String isaEligibilityStatus;
}
