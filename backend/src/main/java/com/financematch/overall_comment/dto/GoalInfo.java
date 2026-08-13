package com.financematch.overall_comment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 종합 코멘트 프롬프트의 goal 섹션. 표기 문자열은 전부 완성된 형태("6억원" 등)로 이미 포맷돼 있다. */
@Getter
@AllArgsConstructor
public class GoalInfo {

    private final String purpose; // "부동산 자금 마련" 등 — common_survey.first_goal_type 라벨
    private final String targetAmount; // "6억원"
    private final String monthsLabel; // "5년" / "18개월"
    private final String expectedAsset; // "2억 7,000만원"
    private final String shortfall; // 미달성 시 부족액("3억 3,000만원"), 달성 시 null
    private final String achievementRate; // "45%"
}
