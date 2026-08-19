package com.financematch.overallcomment.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 종합 코멘트용 축 하나. {@code ratio}는 강약 정렬·판단용 신호일 뿐이고 프롬프트 규칙상 문장에
 * 숫자로 그대로 쓰면 안 된다 — 실제 서술은 {@code facts}(자연어 사실 문장들)만 근거로 한다.
 */
@Getter
@AllArgsConstructor
public class AxisFact {

    private final String name; // 사람이 읽는 축 이름("절세 활용" 등)
    private final double ratio; // 0.0~1.0
    private final List<String> facts;
}
