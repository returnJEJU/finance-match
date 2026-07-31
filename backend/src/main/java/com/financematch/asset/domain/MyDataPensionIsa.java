package com.financematch.asset.domain;

import java.math.BigDecimal;

public record MyDataPensionIsa(
        boolean hasPensionSaving,
        boolean hasIrp,
        boolean hasDc,
        boolean hasIsa,
        BigDecimal pensionSavingBalance,
        BigDecimal irpBalance,
        BigDecimal pensionAnnualPayment,
        BigDecimal irpAnnualPayment,
        BigDecimal dcAnnualPayment,
        BigDecimal isaAnnualDeposit,
        String taxEligibilityStatus,
        String isaEligibilityStatus) {

    public MyDataPensionIsa {
        validateAmount(pensionSavingBalance);
        validateAmount(irpBalance);
        validateAmount(pensionAnnualPayment);
        validateAmount(irpAnnualPayment);
        validateAmount(dcAnnualPayment);
        validateAmount(isaAnnualDeposit);

        if (taxEligibilityStatus == null || isaEligibilityStatus == null) {
            throw new IllegalArgumentException("절세 평가 대상 여부가 필요합니다.");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("연금·ISA 금액은 0보다 작을 수 없습니다.");
        }
    }
}
