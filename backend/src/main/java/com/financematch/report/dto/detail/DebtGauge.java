package com.financematch.report.dto.detail;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DebtGauge {

    private final String label;
    private final BigDecimal value;
    private final int decimals;
    private final String unit;
    private final BigDecimal threshold;
    private final String thresholdLabel;
    private final BigDecimal progress;
    private final String status;
    private final String description;
    private final String amountLabel;
    private final String amount;
}
