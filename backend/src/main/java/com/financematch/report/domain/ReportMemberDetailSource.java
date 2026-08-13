package com.financematch.report.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportMemberDetailSource {

    private Long memberId;
    private String memberName;
    private LocalDate birthDate;
    private BigDecimal annualIncome;
    private BigDecimal monthlyAvailableAmount;
    private String financialKnowledge;
    private String capitalPreservationAttitude;
    private int investmentExperienceScore;
    private BigDecimal financialAsset;
    private BigDecimal totalDebt;
    private BigDecimal annualDebtPayment;
    private boolean hasIsa;
    private boolean hasIrp;
    private boolean hasPensionSaving;
    private BigDecimal isaAnnualDeposit;
    private BigDecimal irpAnnualPayment;
    private BigDecimal dcAnnualPayment;
    private BigDecimal pensionAnnualPayment;
}
