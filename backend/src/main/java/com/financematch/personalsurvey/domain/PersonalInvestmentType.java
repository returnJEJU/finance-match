package com.financematch.personalsurvey.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PersonalInvestmentType {

    STABLE(0),
    STABLE_SEEKING(1),
    NEUTRAL(2),
    AGGRESSIVE(3),
    VERY_AGGRESSIVE(4);

    private final int level;

    // 현재 투자성향이 허용된 최대 단계보다 높으면 최대 단계로 낮춘다.
    public PersonalInvestmentType capAt(PersonalInvestmentType maximum) {
        return level <= maximum.level ? this : maximum;
    }
}
