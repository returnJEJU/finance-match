package com.financematch.personalsurvey.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvestmentExperience {

    LOW_RISK(1.0),
    MODERATE_LOW_RISK(2.5),
    MODERATE_RISK(3.5),
    MODERATE_HIGH_RISK(4.5),
    HIGH_RISK(5.5);

    private final double score;
}
