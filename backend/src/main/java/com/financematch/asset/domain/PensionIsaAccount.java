package com.financematch.asset.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PensionIsaAccount {

    private Long memberId;
    private boolean hasPensionSaving;
    private boolean hasIrp;
    private boolean hasDc;
    private boolean hasIsa;
    private BigDecimal pensionSavingBalance;
    private BigDecimal irpBalance;
    private BigDecimal pensionAnnualPayment;
    private BigDecimal irpAnnualPayment;
    private BigDecimal dcAnnualPayment;
    private BigDecimal isaAnnualDeposit;
    private String taxEligibilityStatus;
    private String isaEligibilityStatus;
    private LocalDateTime linkedAt;
}
