package com.financematch.personalsurvey.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PersonalInvestmentType {

    STABLE(
            0,
            "든든지킴형",
            "높은 수익보다 안정성이 중요해요",
            "금융 선택에서 높은 수익보다 안정성을 더 중요하게 생각해요. 원금 손실 위험이 없고 결과를 가늠하기 쉬운 금융 상품을 선호해요. 예상치 못한 변동이 적을수록 편안함을 느끼는 유형이에요."
    ),
    STABLE_SEEKING(
            1,
            "차곡성장형",
            "안정을 우선하며 수익도 기대해요",
            "안정적인 흐름을 유지하면서도 조금 더 나은 수익을 기대해요. 원금 손실은 되도록 피하고 싶지만, 목표에 도움이 된다면 어느 정도의 변동은 받아들일 수 있어요. 무리하지 않는 선에서 선택의 폭을 넓혀 조금 더 높은 수익을 추구하는 유형이에요."
    ),
    NEUTRAL(2,
            "균형설계형",
            "위험과 수익의 균형을 맞춰요",
            "위험과 수익이 함께 움직인다는 점을 이해하고, 어느 한쪽에 치우치지 않는 선택을 중요하게 생각해요. 현재 여건과 금융 목표를 고려해 위험과 기대 수익 사이에서 균형 잡힌 선택을 하는 유형이에요."
    ),
    AGGRESSIVE(3,
            "적극성장형",
            "위험을 감수해 높은 수익을 추구해요",
            "금융 선택에서 높은 수익을 기대할 수 있다면 비교적 큰 변동과 손실 가능성도 감수할 수 있어요. 위험이 따르더라도 수익성이 높은 선택을 적극적으로 고려하는 유형이에요."
    ),
    VERY_AGGRESSIVE(4,
            "과감도전형",
            "큰 위험도 감수해 고수익에 도전해요",
            "시장 평균을 웃도는 수익을 목표로 과감한 선택을 할 수 있어요. 큰 폭의 변동과 손실 가능성이 있더라도 그에 상응하는 수익을 기대할 수 있다면 주저하지 않아요. 불확실성을 받아들이며 수익성을 우선하는 유형이에요."
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
