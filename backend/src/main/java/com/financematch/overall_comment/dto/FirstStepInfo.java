package com.financematch.overall_comment.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 가장 먼저 개선해보라고 제안할 대상 하나(축 전체가 아니라 구체적인 한 가지 이유+근거). */
@Getter
@AllArgsConstructor
public class FirstStepInfo {

    private final String reason; // "절세 계좌 한도 미달" 같은 한 줄 이유
    private final List<String> facts;
}
