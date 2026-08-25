package com.financematch.overallcomment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.financematch.overallcomment.dto.AxisFact;
import com.financematch.overallcomment.dto.FirstStepInfo;
import com.financematch.overallcomment.dto.GoalInfo;
import com.financematch.overallcomment.dto.NamesInfo;
import com.financematch.overallcomment.dto.OverallCommentPromptInput;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverallCommentToneValidatorTest {

    @Mock private OverallCommentLlmClient llmClient;

    private OverallCommentToneValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OverallCommentToneValidator(llmClient);
    }

    private OverallCommentPromptInput inputWithWeakAxis() {
        NamesInfo names = new NamesInfo("철수", "영희");
        GoalInfo goal = new GoalInfo("주택 마련", "3억", "3년", "6억 8004만원", null, "227%");
        AxisFact strongAxis = new AxisFact("목표 달성 가능성", 1.0, List.of("목표 초과 달성 예상"));
        AxisFact weakAxis = new AxisFact("투자 가치관", 0.68, List.of("두 분의 투자 가치관 일치도 68%"));
        FirstStepInfo firstStep = new FirstStepInfo("투자 가치관", List.of("두 분의 투자 가치관 일치도 68%"));
        return new OverallCommentPromptInput(
                names, goal, List.of(strongAxis), List.of(weakAxis), firstStep, List.of());
    }

    private OverallCommentPromptInput inputWithoutWeakAxis() {
        NamesInfo names = new NamesInfo("철수", "영희");
        GoalInfo goal = new GoalInfo("주택 마련", "3억", "3년", "6억 8004만원", null, "227%");
        AxisFact strongAxis = new AxisFact("목표 달성 가능성", 1.0, List.of("목표 초과 달성 예상"));
        FirstStepInfo firstStep = new FirstStepInfo("목표 달성 가능성", List.of("목표 초과 달성 예상"));
        return new OverallCommentPromptInput(
                names, goal, List.of(strongAxis), List.of(), firstStep, List.of());
    }

    @Test
    void 위반이_없으면_통과한다() {
        when(llmClient.checkTone(anyString())).thenReturn("{\"valid\": true, \"violations\": []}");

        OverallCommentToneValidator.ValidationResult result =
                validator.validate("**목표 달성 가능성이 탄탄해요**.", inputWithWeakAxis());

        assertTrue(result.valid());
        assertTrue(result.violations().isEmpty());
    }

    @Test
    void 위반이_있으면_잡아낸다() {
        when(llmClient.checkTone(anyString()))
                .thenReturn("{\"valid\": false, \"violations\": [\"투자 가치관 문장이 긍정 일변도\"]}");

        OverallCommentToneValidator.ValidationResult result =
                validator.validate("**투자 가치관은 문제없어요**.", inputWithWeakAxis());

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("투자 가치관")));
    }

    @Test
    void LLM_호출이_실패하면_위반으로_처리한다() {
        when(llmClient.checkTone(anyString()))
                .thenThrow(new OverallCommentLlmClient.LlmCallException("호출 실패"));

        OverallCommentToneValidator.ValidationResult result =
                validator.validate("**목표 달성 가능성이 탄탄해요**.", inputWithWeakAxis());

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("호출 실패")));
    }

    @Test
    void 응답이_깨진_JSON이면_위반으로_처리한다() {
        when(llmClient.checkTone(anyString())).thenReturn("이건 JSON이 아님");

        OverallCommentToneValidator.ValidationResult result =
                validator.validate("**목표 달성 가능성이 탄탄해요**.", inputWithWeakAxis());

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("파싱 실패")));
    }

    @Test
    void valid_필드가_없으면_위반으로_처리한다() {
        when(llmClient.checkTone(anyString())).thenReturn("{\"violations\": []}");

        OverallCommentToneValidator.ValidationResult result =
                validator.validate("**목표 달성 가능성이 탄탄해요**.", inputWithWeakAxis());

        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("파싱 실패")));
    }

    @Test
    void 강점_약점_축_이름을_프롬프트에_담아_LLM에_넘긴다() {
        when(llmClient.checkTone(anyString())).thenReturn("{\"valid\": true, \"violations\": []}");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        validator.validate("**목표 달성 가능성이 탄탄해요**.", inputWithWeakAxis());

        org.mockito.Mockito.verify(llmClient).checkTone(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("목표 달성 가능성"));
        assertTrue(prompt.contains("투자 가치관"));
        assertTrue(prompt.contains("**목표 달성 가능성이 탄탄해요**."));
    }

    @Test
    void weakAxes가_비어있으면_없음으로_표시해_프롬프트를_만든다() {
        when(llmClient.checkTone(anyString())).thenReturn("{\"valid\": true, \"violations\": []}");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        validator.validate("**목표 달성 가능성이 탄탄해요**.", inputWithoutWeakAxis());

        org.mockito.Mockito.verify(llmClient).checkTone(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("(없음)"));
    }
}
