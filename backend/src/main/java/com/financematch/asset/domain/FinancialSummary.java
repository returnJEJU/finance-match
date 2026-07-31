package com.financematch.asset.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FinancialSummary {

    private Long memberId;
    private BigDecimal financialAsset;
    private BigDecimal totalDebt;
    private BigDecimal availableBalance;
    private BigDecimal annualDebtPayment;
    private BigDecimal averageInterestRate;
    private boolean hasHighRateDebt;
    private LocalDateTime linkedAt;
}
