package com.financematch.overallcomment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.overallcomment.dto.OverallCommentPromptInput;
import com.financematch.report.mapper.ReportWriteMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverallCommentServiceTest {

    @Mock private OverallCommentLlmClient llmClient;
    @Mock private OverallCommentPromptBuilder promptBuilder;
    @Mock private OverallCommentRuleValidator ruleValidator;
    @Mock private OverallCommentValidator overallCommentValidator;
    @Mock private ReportWriteMapper reportWriteMapper;

    @InjectMocks private OverallCommentService service;

    private OverallCommentPromptInput input;

    @BeforeEach
    void setUp() {
        input = mock(OverallCommentPromptInput.class);
        when(promptBuilder.build(input)).thenReturn("프롬프트");
    }

    private OverallCommentRuleValidator.ValidationResult validRule() {
        return new OverallCommentRuleValidator.ValidationResult(true, List.of());
    }

    private OverallCommentRuleValidator.ValidationResult invalidRule() {
        return new OverallCommentRuleValidator.ValidationResult(false, List.of("길이 초과"));
    }

    private OverallCommentValidator.ValidationResult validAxis() {
        return new OverallCommentValidator.ValidationResult(true, List.of());
    }

    private OverallCommentValidator.ValidationResult invalidAxis() {
        return new OverallCommentValidator.ValidationResult(false, List.of("허용되지 않은 숫자 등장: 5000만원"));
    }

    @Test
    void 첫_시도에_성공하면_바로_결과를_반환한다() {
        when(llmClient.generateComment("프롬프트")).thenReturn("{\"paragraphs\": [\"문단1\", \"문단2\"]}");
        when(ruleValidator.validate(anyString(), anyInt())).thenReturn(validRule());
        when(overallCommentValidator.validate(anyString(), org.mockito.ArgumentMatchers.eq(input)))
                .thenReturn(validAxis());

        Optional<String> result = service.generate(input);

        assertTrue(result.isPresent());
        assertEquals("문단1\n\n문단2", result.get());
        verify(llmClient, times(1)).generateComment("프롬프트");
    }

    @Test
    void 마크다운_코드펜스로_감싸진_출력도_벗겨서_파싱한다() {
        when(llmClient.generateComment("프롬프트"))
                .thenReturn("```json\n{\"paragraphs\": [\"문단1\"]}\n```");
        when(ruleValidator.validate(anyString(), anyInt())).thenReturn(validRule());
        when(overallCommentValidator.validate(anyString(), org.mockito.ArgumentMatchers.eq(input)))
                .thenReturn(validAxis());

        Optional<String> result = service.generate(input);

        assertEquals("문단1", result.get());
    }

    @Test
    void LLM_호출이_실패하면_재시도하고_다음_시도에서_성공하면_그대로_반환한다() {
        when(llmClient.generateComment("프롬프트"))
                .thenThrow(new OverallCommentLlmClient.LlmCallException("호출 실패"))
                .thenReturn("{\"paragraphs\": [\"문단1\"]}");
        when(ruleValidator.validate(anyString(), anyInt())).thenReturn(validRule());
        when(overallCommentValidator.validate(anyString(), org.mockito.ArgumentMatchers.eq(input)))
                .thenReturn(validAxis());

        Optional<String> result = service.generate(input);

        assertTrue(result.isPresent());
        verify(llmClient, times(2)).generateComment("프롬프트");
    }

    @Test
    void paragraphs_필드가_없으면_파싱_실패로_재시도한다() {
        when(llmClient.generateComment("프롬프트"))
                .thenReturn("{\"other\": \"x\"}")
                .thenReturn("{\"paragraphs\": [\"문단1\"]}");
        when(ruleValidator.validate(anyString(), anyInt())).thenReturn(validRule());
        when(overallCommentValidator.validate(anyString(), org.mockito.ArgumentMatchers.eq(input)))
                .thenReturn(validAxis());

        Optional<String> result = service.generate(input);

        assertTrue(result.isPresent());
        verify(llmClient, times(2)).generateComment("프롬프트");
    }

    @Test
    void paragraphs가_빈_배열이면_파싱_실패로_취급한다() {
        when(llmClient.generateComment("프롬프트")).thenReturn("{\"paragraphs\": []}");

        Optional<String> result = service.generate(input);

        assertFalse(result.isPresent());
        verify(llmClient, times(3)).generateComment("프롬프트");
    }

    @Test
    void paragraphs가_배열이_아니면_파싱_실패로_취급한다() {
        when(llmClient.generateComment("프롬프트"))
                .thenReturn("{\"paragraphs\": \"배열이 아님\"}")
                .thenReturn("{\"paragraphs\": [\"문단1\"]}");
        when(ruleValidator.validate(anyString(), anyInt())).thenReturn(validRule());
        when(overallCommentValidator.validate(anyString(), org.mockito.ArgumentMatchers.eq(input)))
                .thenReturn(validAxis());

        Optional<String> result = service.generate(input);

        assertTrue(result.isPresent());
        verify(llmClient, times(2)).generateComment("프롬프트");
    }

    @Test
    void 깨진_JSON이면_파싱_실패로_취급하고_결국_모두_실패하면_빈값을_반환한다() {
        when(llmClient.generateComment("프롬프트")).thenReturn("이건 JSON이 아님");

        Optional<String> result = service.generate(input);

        assertFalse(result.isPresent());
        verify(llmClient, times(3)).generateComment("프롬프트");
    }

    @Test
    void 공통_규칙_검증에_실패하면_재시도한다() {
        when(llmClient.generateComment("프롬프트")).thenReturn("{\"paragraphs\": [\"문단1\"]}");
        when(ruleValidator.validate(anyString(), anyInt())).thenReturn(invalidRule()).thenReturn(validRule());
        when(overallCommentValidator.validate(anyString(), org.mockito.ArgumentMatchers.eq(input)))
                .thenReturn(validAxis());

        Optional<String> result = service.generate(input);

        assertTrue(result.isPresent());
        verify(llmClient, times(2)).generateComment("프롬프트");
    }

    @Test
    void 축_커버리지_검증에_실패하면_재시도한다() {
        when(llmClient.generateComment("프롬프트")).thenReturn("{\"paragraphs\": [\"문단1\"]}");
        when(ruleValidator.validate(anyString(), anyInt())).thenReturn(validRule());
        when(overallCommentValidator.validate(anyString(), org.mockito.ArgumentMatchers.eq(input)))
                .thenReturn(invalidAxis())
                .thenReturn(validAxis());

        Optional<String> result = service.generate(input);

        assertTrue(result.isPresent());
        verify(llmClient, times(2)).generateComment("프롬프트");
    }

    @Test
    void 세번_모두_검증에_실패하면_빈값을_반환한다() {
        when(llmClient.generateComment("프롬프트")).thenReturn("{\"paragraphs\": [\"문단1\"]}");
        when(ruleValidator.validate(anyString(), anyInt())).thenReturn(validRule());
        when(overallCommentValidator.validate(anyString(), org.mockito.ArgumentMatchers.eq(input)))
                .thenReturn(invalidAxis());

        Optional<String> result = service.generate(input);

        assertFalse(result.isPresent());
        verify(llmClient, times(3)).generateComment("프롬프트");
    }

    @Test
    void 생성에_성공하면_리포트에_저장한다() {
        when(llmClient.generateComment("프롬프트")).thenReturn("{\"paragraphs\": [\"문단1\"]}");
        when(ruleValidator.validate(anyString(), anyInt())).thenReturn(validRule());
        when(overallCommentValidator.validate(anyString(), org.mockito.ArgumentMatchers.eq(input)))
                .thenReturn(validAxis());

        service.generateAndSave(1L, input);

        verify(reportWriteMapper).upsertExpertComment(1L, "문단1");
    }

    @Test
    void 생성에_실패하면_저장을_건너뛴다() {
        when(llmClient.generateComment("프롬프트")).thenReturn("{\"paragraphs\": []}");

        service.generateAndSave(1L, input);

        verify(reportWriteMapper, never()).upsertExpertComment(org.mockito.ArgumentMatchers.anyLong(), anyString());
    }
}
