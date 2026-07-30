package com.financematch.report.llm;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 모든 축에 공통으로 적용되는 규칙(AGENTIC.md R1·R5)만 검사한다.
 *
 * <p>축별 규칙(R4 이름 사용 여부 등)은 축마다 다르므로 각 {@code *ReasonService}가 직접 검사한다 —
 * 프롬프트가 공통 규칙/축별 규칙으로 나뉜 것과 같은 구조다.
 */
@Component
public class ReasonRuleValidator {

    private static final List<String> FORBIDDEN_VIEWER_WORDS =
            List.of("당신", "귀하", "배우자님", "남편분", "아내분", "상대방", "파트너님");

    private static final List<String> FORBIDDEN_PROMISE_WORDS = List.of("반드시", "수익이 납니다", "보장");

    // R5 기본 길이. 절세 축은 두 사람·여러 계좌를 같이 언급할 수 있어 팀 확정(GAP-09)으로 예외를 둔다.
    private static final int DEFAULT_MAX_LENGTH = 100;

    public ValidationResult validateCommon(String reason) {
        return validateCommon(reason, DEFAULT_MAX_LENGTH);
    }

    public ValidationResult validateCommon(String reason, int maxLength) {
        List<String> violations = new ArrayList<>();

        for (String word : FORBIDDEN_VIEWER_WORDS) {
            if (reason.contains(word)) {
                violations.add("뷰어 종속 표현 포함: " + word);
            }
        }
        for (String word : FORBIDDEN_PROMISE_WORDS) {
            if (reason.contains(word)) {
                violations.add("단정적 표현 포함: " + word);
            }
        }
        if (reason.length() > maxLength) {
            violations.add("길이 초과(" + maxLength + "자): " + reason.length() + "자");
        }

        return new ValidationResult(violations.isEmpty(), violations);
    }

    public record ValidationResult(boolean valid, List<String> violations) {}
}
