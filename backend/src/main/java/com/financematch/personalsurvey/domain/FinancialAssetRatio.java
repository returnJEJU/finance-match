package com.financematch.personalsurvey.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FinancialAssetRatio {

    UNDER_10(0.5),
    UNDER_30(1.0),
    UNDER_50(1.5),
    UNDER_80(2.0),
    OVER_80(2.5);

    private final double score;
}
