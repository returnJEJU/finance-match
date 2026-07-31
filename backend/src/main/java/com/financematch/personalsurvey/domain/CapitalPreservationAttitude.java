package com.financematch.personalsurvey.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CapitalPreservationAttitude {

    ZERO(2.0),
    UNDER_10(6.0),
    UNDER_20(8.0),
    UNDER_50(10.0),
    UNDER_70(12.0),
    FULL(14.0);

    private final double score;
}
