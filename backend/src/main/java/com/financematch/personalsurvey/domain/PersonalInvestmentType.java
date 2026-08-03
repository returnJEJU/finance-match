package com.financematch.personalsurvey.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PersonalInvestmentType {

    STABLE(
            0,
            "든든지킴형",
            "안정형 임시 헤드라인 (확정 후 수정)",
            "안정형 임시 설명 (확정 후 수정)"
    ),
    STABLE_SEEKING(
            1,
            "차곡성장형",
            "안정추구형 임시 헤드라인 (확정 후 수정)",
            "안정추구형 임시 설명 (확정 후 수정)"
    ),
    NEUTRAL(2,
            "균형설계형",
            "위험중립형 임시 헤드라인 (확정 후 수정)",
            "위험중립형 임시 설명 (확정 후 수정)"
    ),
    AGGRESSIVE(3,
            "적극성장형",
            "적극투자형 임시 헤드라인 (확정 후 수정)",
            "적극투자형 임시 설명 (확정 후 수정)"
    ),
    VERY_AGGRESSIVE(4,
            "과감도전형",
            "공격투자형 임시 헤드라인 (확정 후 수정)",
            "공격투자형 임시 설명 (확정 후 수정)"
    );

    private final int level;
    private final String koreanName;
    private final String headline;
    private final String description;

    // 현재 투자성향이 허용된 최대 단계보다 높으면 최대 단계로 낮춘다.
    public PersonalInvestmentType capAt(PersonalInvestmentType maximum) {
        return level <= maximum.level ? this : maximum;
    }
}
