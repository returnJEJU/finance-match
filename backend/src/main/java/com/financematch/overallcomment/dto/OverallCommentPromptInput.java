package com.financematch.overallcomment.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 종합 코멘트 프롬프트에 넣을 재료. {@code OverallCommentInputBuilder}가 조립하고, {@code
 * OverallCommentPromptBuilder}가 이 값을 그대로 JSON으로 직렬화해 프롬프트의 [입력 데이터]
 * 섹션에 붙인다. {@code allowedNumbers}에 없는 숫자가 출력에 등장하면 검증에서 걸러진다({@code
 * OverallCommentValidator}).
 */
@Getter
@AllArgsConstructor
public class OverallCommentPromptInput {

    private final NamesInfo names;
    private final GoalInfo goal;
    private final List<AxisFact> strongAxes; // 비율 상위 3개
    private final List<AxisFact> weakAxes; // 비율 하위 2개(절세 축 미해당이면 1개)
    private final FirstStepInfo firstStep;
    private final List<String> allowedNumbers;
}
