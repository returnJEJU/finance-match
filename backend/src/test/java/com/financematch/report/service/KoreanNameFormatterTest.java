package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * 리포트 문구에 쓰는 이름 축약.
 *
 * <p>성을 떼고 부르는 규칙이다("김철수" → "철수"). 리포트 문구 곳곳에서 쓰이므로 여기서 예외가 나면
 * 리포트 전체가 안 뜬다 — 이름이 비어 있거나 한 글자인 경우를 그대로 통과시키는 것이 그래서 중요하다.
 */
class KoreanNameFormatterTest {

    @Test
    void 성을_떼고_이름만_남긴다() {
        assertEquals("철수", KoreanNameFormatter.abbreviate("김철수"));
        assertEquals("영희", KoreanNameFormatter.abbreviate("이영희"));
    }

    @Test
    void 두_글자_이름도_성을_뗀다() {
        // "박수" 같은 두 글자 이름은 규칙대로 한 글자가 된다.
        assertEquals("수", KoreanNameFormatter.abbreviate("박수"));
    }

    @Test
    void 한_글자_이름은_그대로_둔다() {
        // 떼면 빈 문자열이 되어 "님은 ..." 처럼 주어가 사라진 문구가 나간다.
        assertEquals("수", KoreanNameFormatter.abbreviate("수"));
    }

    @Test
    void 빈_이름도_그대로_둔다() {
        assertEquals("", KoreanNameFormatter.abbreviate(""));
    }

    @Test
    void 이름이_없으면_예외를_내지_않고_그대로_돌려준다() {
        // 여기서 NullPointerException 이 나면 리포트 응답 전체가 실패한다.
        assertNull(KoreanNameFormatter.abbreviate(null));
    }
}
