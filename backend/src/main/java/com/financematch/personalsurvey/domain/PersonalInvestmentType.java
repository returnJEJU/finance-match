package com.financematch.personalsurvey.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PersonalInvestmentType {

    STABLE(
            0,
            "안정형",
            "안정형 임시 설명 (확정 후 수정)",
            "type-stable.png"
    ),
    STABLE_SEEKING(
            1,
            "안정추구형",
            "안정추구형 임시 설명 (확정 후 수정)",
            "type-stability-seeking.png"
    ),
    NEUTRAL(2,
            "위험중립형",
            "위험중립형 임시 설명 (확정 후 수정)",
            "type-risk-neutral.png"
    ),
    AGGRESSIVE(3,
            "적극투자형",
            "적극투자형 임시 설명 (확정 후 수정)",
            "type-active.png"
    ),
    VERY_AGGRESSIVE(4,
            "공격투자형",
            "공격투자형 임시 설명 (확정 후 수정)",
            "type-aggressive.png"
    );

    private final int level;

    private final String koreanName;
    private final String description;
    private final String characterImageName;

    // 현재 투자성향이 허용된 최대 단계보다 높으면 최대 단계로 낮춘다.
    public PersonalInvestmentType capAt(PersonalInvestmentType maximum) {
        return level <= maximum.level ? this : maximum;
    }
}
