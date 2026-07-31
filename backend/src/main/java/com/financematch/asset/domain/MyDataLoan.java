package com.financematch.asset.domain;

import java.math.BigDecimal;

public record MyDataLoan(
        String institutionName,
        String productName,
        BigDecimal balance,
        BigDecimal annualPayment,
        BigDecimal interestRate,
        boolean highRateDebt) {

    public MyDataLoan {
        if (institutionName == null
                || institutionName.isBlank()
                || productName == null
                || productName.isBlank()
                || balance == null
                || balance.signum() < 0
                || annualPayment == null
                || annualPayment.signum() < 0
                || interestRate == null
                || interestRate.signum() < 0) {
            throw new IllegalArgumentException("마이데이터 대출 정보가 올바르지 않습니다.");
        }
    }
}
