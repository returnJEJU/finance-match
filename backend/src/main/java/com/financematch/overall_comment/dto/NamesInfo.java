package com.financematch.overall_comment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 성을 뗀 축약형 이름("민준" 등). '님'은 붙이지 않는다 — 모델이 프롬프트 규칙에 따라 직접 붙인다. */
@Getter
@AllArgsConstructor
public class NamesInfo {

    private final String me;
    private final String partner;
}
