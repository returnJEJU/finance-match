package com.financematch.overallcomment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.overallcomment.dto.AxisFact;
import com.financematch.overallcomment.dto.FirstStepInfo;
import com.financematch.overallcomment.dto.GoalInfo;
import com.financematch.overallcomment.dto.NamesInfo;
import com.financematch.overallcomment.dto.OverallCommentPromptInput;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverallCommentValidatorTest {

    private final OverallCommentValidator validator = new OverallCommentValidator();

    @Test
    void allowedNumbers에_있는_숫자만_쓰면_통과한다() {
        OverallCommentPromptInput input = input(List.of("6억 8004만원", "3억", "68%"));
        String comment = "**목표 달성 가능성이 탄탄해요**. 6억 8004만원까지 예상되고, 일치도는 68%예요.";

        OverallCommentValidator.ValidationResult result = validator.validate(comment, input);

        assertTrue(result.valid());
    }

    @Test
    void 단위를_조합해서_다른_숫자를_만들면_잡아낸다() {
        // 실제로 관찰된 버그: expectedAsset="6억 8004만원"인데 targetAmount="3억"의 "3억"과
        // expectedAsset의 "8004만원"을 조합한 "3억 8004만원"을 모델이 그대로 써서 통과해버렸다.
        OverallCommentPromptInput input = input(List.of("6억 8004만원", "3억"));
        String comment = "**목표 달성 가능성이 탄탄해요**. 3억 8004만원에 도달할 것으로 예상돼요.";

        OverallCommentValidator.ValidationResult result = validator.validate(comment, input);

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("3억 8004만원")));
    }

    @Test
    void allowedNumbers에_없는_숫자를_쓰면_잡아낸다() {
        OverallCommentPromptInput input = input(List.of("6억 8004만원"));
        String comment = "**목표 달성 가능성이 탄탄해요**. 7억원까지 예상돼요.";

        OverallCommentValidator.ValidationResult result = validator.validate(comment, input);

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("7억")));
    }

    @Test
    void weakAxes가_있는데_문단이_분리되지_않으면_잡아낸다() {
        // 실제로 관찰된 버그: 프롬프트가 강점/약점 문단을 나누라고 지시해도 모델이 가끔 "\n\n" 없이
        // 전부 한 문단으로 붙여 써서 강점·약점 구분이 안 됨.
        OverallCommentPromptInput input = inputWithWeakAxis(List.of("68%"));
        String comment =
                "**목표 달성 가능성이 탄탄해요**. 목표를 초과 달성할 것으로 예상돼요."
                        + " **투자 가치관은 보완이 필요해요**. 일치도는 68%예요.";

        OverallCommentValidator.ValidationResult result = validator.validate(comment, input);

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("문단이 분리되지 않음")));
    }

    @Test
    void weakAxes가_있어도_문단이_분리되면_통과한다() {
        OverallCommentPromptInput input = inputWithWeakAxis(List.of("68%"));
        String comment =
                "**목표 달성 가능성이 탄탄해요**. 목표를 초과 달성할 것으로 예상돼요.\n\n"
                        + "**투자 가치관은 보완이 필요해요**. 일치도는 68%예요.";

        OverallCommentValidator.ValidationResult result = validator.validate(comment, input);

        assertTrue(result.valid());
    }

    @Test
    void 이름_뒤에_님_없이_조사가_바로_붙으면_잡아낸다() {
        // 실제로 관찰된 버그: names는 '님' 없는 축약형("하나")인데 모델이 "하나는 2000만원"처럼
        // '님'을 빼먹고 조사를 바로 붙였다.
        OverallCommentPromptInput input = input(List.of("2000만원"));
        String comment = "**목표 달성 가능성이 탄탄해요**. 철수는 2000만원을 상환하고 있어요.";

        OverallCommentValidator.ValidationResult result = validator.validate(comment, input);

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("'님'이 빠짐") && v.contains("철수")));
    }

    @Test
    void 이름_뒤에_님이_붙어있으면_통과한다() {
        OverallCommentPromptInput input = input(List.of("2000만원"));
        String comment = "**목표 달성 가능성이 탄탄해요**. 철수님은 2000만원을 상환하고 있어요.";

        OverallCommentValidator.ValidationResult result = validator.validate(comment, input);

        assertTrue(result.valid());
    }

    private OverallCommentPromptInput input(List<String> allowedNumbers) {
        NamesInfo names = new NamesInfo("철수", "영희");
        GoalInfo goal = new GoalInfo("주택 마련", "3억", "3년", "6억 8004만원", null, "227%");
        AxisFact strongAxis = new AxisFact("목표 달성 가능성", 1.0, List.of("목표 초과 달성 예상"));
        FirstStepInfo firstStep = new FirstStepInfo("목표 달성 가능성", List.of("목표 초과 달성 예상"));
        return new OverallCommentPromptInput(
                names, goal, List.of(strongAxis), List.of(), firstStep, allowedNumbers);
    }

    private OverallCommentPromptInput inputWithWeakAxis(List<String> allowedNumbers) {
        NamesInfo names = new NamesInfo("철수", "영희");
        GoalInfo goal = new GoalInfo("주택 마련", "3억", "3년", "6억 8004만원", null, "227%");
        AxisFact strongAxis = new AxisFact("목표 달성 가능성", 1.0, List.of("목표 초과 달성 예상"));
        AxisFact weakAxis = new AxisFact("투자 가치관", 0.68, List.of("두 분의 투자 가치관 일치도 68%"));
        FirstStepInfo firstStep = new FirstStepInfo("투자 가치관", List.of("두 분의 투자 가치관 일치도 68%"));
        return new OverallCommentPromptInput(
                names, goal, List.of(strongAxis), List.of(weakAxis), firstStep, allowedNumbers);
    }
}
