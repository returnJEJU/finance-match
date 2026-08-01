package com.financematch.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 리포트 화면의 목표 달성 가능성 카드(진행 현황 바)에 쓰는 값. */
@Getter
@AllArgsConstructor
public class GoalProgress {

    private final boolean isAchieved;
    private final String amountLabel;
    private final String barLabel;
    private final String availableAsset;
    private final String achievementRate;
}
