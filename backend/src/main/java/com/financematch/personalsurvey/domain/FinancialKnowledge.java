package com.financematch.personalsurvey.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FinancialKnowledge {

    VERY_LOW(1.0),
    LOW(2.5),
    MEDIUM(3.5),
    HIGH(4.0),
    VERY_HIGH(5.5);

    private final double score;
}
