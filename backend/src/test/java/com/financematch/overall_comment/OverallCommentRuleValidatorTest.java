package com.financematch.overall_comment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OverallCommentRuleValidatorTest {

    private final OverallCommentRuleValidator validator = new OverallCommentRuleValidator();

    @Test
    void 모든_문장이_해요체로_끝나면_통과한다() {
        String comment =
                "**목표 달성 가능성을 잘 챙기고 있어요**. 3년 뒤 목표의 85% 도달이 예상돼요.\n\n"
                        + "**절세 활용이 잘 맞아요**. 남은 한도를 채워보세요.";

        OverallCommentRuleValidator.ValidationResult result = validator.validate(comment, 550);

        assertTrue(result.valid());
        assertTrue(result.violations().isEmpty());
    }

    @Test
    void 다체로_끝나는_문장이_있으면_위반으로_잡는다() {
        String comment = "**투자 가치관은 보완이 필요하다**. 두 분의 투자 가치관 일치도 68%로 아직 차이가 있어요.";

        OverallCommentRuleValidator.ValidationResult result = validator.validate(comment, 550);

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("해요체 위반")));
    }

    @Test
    void 명사형_종결로_끝나는_문장이_있으면_위반으로_잡는다() {
        String comment = "**금융 자산이 탄탄해요**. 여력을 활용하는 방안 검토함.";

        OverallCommentRuleValidator.ValidationResult result = validator.validate(comment, 550);

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("해요체 위반")));
    }

    @Test
    void 세_분이라고_인원수를_잘못_말하면_위반으로_잡는다() {
        String comment = "**금융 자산이 탄탄해요**. 세 분의 합산 금융자산이 3억 250만원으로 높은 수준이에요.";

        OverallCommentRuleValidator.ValidationResult result = validator.validate(comment, 550);

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("잘못된 인원 수 표현")));
    }

    @Test
    void 두_분이라고_정확히_말하면_통과한다() {
        String comment = "**금융 자산이 탄탄해요**. 두 분의 합산 금융자산이 3억 250만원으로 높은 수준이에요.";

        OverallCommentRuleValidator.ValidationResult result = validator.validate(comment, 550);

        assertTrue(result.valid());
    }
}
