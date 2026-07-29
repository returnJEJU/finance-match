package com.financematch.match.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchMemberData {

    private Long memberId;
    private String memberName;
    private LocalDate birthDate;

    // 연령대별 금융자산 중앙값
    private BigDecimal ageGroupAssetMedian;

    // 개인 설문
    private BigDecimal annualIncome;
    private BigDecimal monthlyAvailableAmount;
    private String financialAssetRatio;
    private String financialKnowledge;
    private String capitalPreservationAttitude;

    // 투자 경험에서 가장 높은 위험등급 점수
    private int investmentExperienceScore;

    // 금융자산·부채
    private BigDecimal financialAsset;
    private BigDecimal totalDebt;
    private BigDecimal annualDebtPayment;

    // 연금·ISA
    private BigDecimal pensionSavingBalance;
    private BigDecimal irpBalance;
    private BigDecimal pensionAnnualPayment;
    private BigDecimal irpAnnualPayment;
    private BigDecimal dcAnnualPayment;
    private BigDecimal isaAnnualDeposit;
    private String taxEligibilityStatus;
    private String isaEligibilityStatus;
}