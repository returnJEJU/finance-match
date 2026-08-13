package com.financematch.overall_comment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 종합 코멘트 출력에 공통으로 적용되는 규칙만 검사한다(뷰어 독립 표현, 단정적 투자 권유 표현,
 * 해요체 어미, 길이). 숫자·축 커버 여부 같은 종합 코멘트 전용 규칙은 {@link OverallCommentValidator}가
 * 본다.
 */
@Component
public class OverallCommentRuleValidator {

    private static final List<String> FORBIDDEN_VIEWER_WORDS =
            List.of("당신", "귀하", "배우자님", "남편분", "아내분", "상대방", "파트너님");

    private static final List<String> FORBIDDEN_PROMISE_WORDS = List.of("반드시", "수익이 납니다", "보장");

    // 커플은 항상 정확히 두 사람이다. 모델이 가끔 "두 분"을 "세 분"으로 잘못 쓰는 걸 실제로
    // 확인해서 추가했다(실제 출력 예: "세 분의 합산 금융자산이...").
    private static final List<String> FORBIDDEN_HEADCOUNT_WORDS = List.of("세 분", "세분", "네 분", "네분");

    // 문장 끝(.!?) 뒤 공백/줄바꿈에서 나눈다. 입력 숫자는 전부 "1억 2000만원"류 한글 표기라 소수점
    // 마침표가 섞일 일이 없어 단순 분리로 충분하다.
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s*");

    // 해요체는 이름 그대로 항상 '요'로 끝난다("~해요", "~이에요/예요", "~있어요", "~돼요", "~같아요").
    // 다체("~다")·명사형 종결("~함", "~됨", "~음")로 끝나면 규칙 위반이다.
    private static final char HAEYO_ENDING = '요';

    public ValidationResult validate(String comment, int maxLength) {
        List<String> violations = new ArrayList<>();

        for (String word : FORBIDDEN_VIEWER_WORDS) {
            if (comment.contains(word)) {
                violations.add("뷰어 종속 표현 포함: " + word);
            }
        }
        for (String word : FORBIDDEN_PROMISE_WORDS) {
            if (comment.contains(word)) {
                violations.add("단정적 표현 포함: " + word);
            }
        }
        for (String word : FORBIDDEN_HEADCOUNT_WORDS) {
            if (comment.contains(word)) {
                violations.add("잘못된 인원 수 표현 포함: " + word);
            }
        }
        checkHaeyoEnding(comment, violations);
        if (comment.length() > maxLength) {
            violations.add("길이 초과(" + maxLength + "자): " + comment.length() + "자");
        }

        return new ValidationResult(violations.isEmpty(), violations);
    }

    private void checkHaeyoEnding(String comment, List<String> violations) {
        for (String rawSentence : SENTENCE_SPLIT.split(comment)) {
            String sentence = stripEdges(rawSentence);
            if (sentence.isEmpty()) {
                continue;
            }
            char lastChar = sentence.charAt(sentence.length() - 1);
            if (lastChar != HAEYO_ENDING) {
                violations.add("해요체 위반(다체·명사형 종결로 끝남): " + rawSentence.trim());
            }
        }
    }

    // 문장 앞뒤의 **볼드 마커, 따옴표, 문장부호, 공백을 걷어내고 실제 어미만 남긴다.
    private String stripEdges(String sentence) {
        String trimmed = sentence.trim();
        trimmed = trimmed.replaceAll("^[*\"'\\s]+", "");
        trimmed = trimmed.replaceAll("[*\"'.!?\\s]+$", "");
        return trimmed;
    }

    public record ValidationResult(boolean valid, List<String> violations) {}
}
