package com.financematch.report.dto.detail;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssetDetail {

    private final String referenceLabel;
    private final BigDecimal referenceValue;
    private final BigDecimal referenceProgress;
    private final String currentLabel;
    private final BigDecimal currentValue;
    private final BigDecimal progress;
    private final String note;
}
