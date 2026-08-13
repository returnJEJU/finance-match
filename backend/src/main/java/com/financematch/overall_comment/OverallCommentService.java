package com.financematch.overall_comment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.overall_comment.dto.OverallCommentPromptInput;
import com.financematch.report.mapper.ReportWriteMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 종합 코멘트를 GPT로 생성해 {@code report.expert_comment}에 저장한다. 규칙 위반 시 최대
 * {@value #MAX_ATTEMPTS}회 재시도하고, 그래도 전부 실패하면 저장을 건너뛴다 — 5축 reason과 달리
 * 결정론적 fallback 문구가 없다(여러 축을 종합해 자연스러운 문단으로 엮는 게 목적이라 규칙만으로
 * 조립하면 어색해지기 쉽다). report 화면(ReportService)은 5축 reason만 완성 여부 판단에 쓰고
 * expert_comment는 nullable로 두므로, 이 코멘트가 비어 있어도 리포트 자체는 이미 완성 상태로
 * 보여줄 수 있다.
 *
 * <p>모델 출력은 {@code {"paragraphs": ["문단1", "문단2"]}} 형태다 — {@link OverallCommentLlmClient}가
 * OpenAI Responses API의 Structured Outputs(json_schema strict)로 이 형태를 강제하므로, 파싱
 * 실패는 사실상 나오지 않는다(그래도 방어적으로 파싱 실패 시 재시도는 남겨뒀다). 문단들은 저장
 * 전에 "\n\n"으로 합쳐 하나의 문자열로 만든다.
 *
 * <p>{@link #generateAndSave}는 {@code @Async}(RootConfig의 기본 {@code taskExecutor})로 돈다 —
 * LLM 호출(최대 90초 × 재시도 {@value #MAX_ATTEMPTS}회)이 {@code MatchService.
 * getOrCalculateCompatibilityResult}의 응답(궁합 계산 API·설문 완료 트리거)을 막지 않게 하기
 * 위함이다. 5축 reason·compatibility_result는 이미 별도로 저장이 끝난 뒤라, 이 메서드가 늦게
 * 끝나거나 실패해도 그 값들에는 영향이 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverallCommentService {

    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_LENGTH = 550;

    private final OverallCommentLlmClient llmClient;
    private final OverallCommentPromptBuilder promptBuilder;
    private final OverallCommentRuleValidator ruleValidator;
    private final OverallCommentValidator overallCommentValidator;
    private final ReportWriteMapper reportWriteMapper;
    private final ObjectMapper mapper = new ObjectMapper();

    /** {@link #generate}로 만든 결과를 {@code report.expert_comment}에 저장한다. 실패하면 저장을 건너뛴다. */
    @Async
    public void generateAndSave(Long compatibilityResultId, OverallCommentPromptInput input) {
        generate(input)
                .ifPresentOrElse(
                        comment -> reportWriteMapper.upsertExpertComment(compatibilityResultId, comment),
                        () -> log.warn("종합 코멘트가 비어 있어 저장을 건너뜁니다(compatibilityResultId={})", compatibilityResultId));
    }

    public Optional<String> generate(OverallCommentPromptInput input) {
        String prompt = promptBuilder.build(input);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String rawOutput;
            try {
                rawOutput = llmClient.generateComment(prompt);
            } catch (OverallCommentLlmClient.LlmCallException e) {
                log.warn("종합 코멘트 LLM 호출 실패(시도 {}/{})", attempt, MAX_ATTEMPTS, e);
                continue;
            }

            List<String> paragraphs = parseParagraphs(rawOutput);
            if (paragraphs == null) {
                log.warn("종합 코멘트 출력 파싱 실패(시도 {}/{}): {}", attempt, MAX_ATTEMPTS, rawOutput);
                continue;
            }
            String fullText = String.join("\n\n", paragraphs);

            OverallCommentRuleValidator.ValidationResult commonResult = ruleValidator.validate(fullText, MAX_LENGTH);
            if (!commonResult.valid()) {
                log.warn(
                        "종합 코멘트 공통 규칙 검증 실패(시도 {}/{}): {} — {}",
                        attempt,
                        MAX_ATTEMPTS,
                        fullText,
                        commonResult.violations());
                continue;
            }

            OverallCommentValidator.ValidationResult result = overallCommentValidator.validate(fullText, input);
            if (result.valid()) {
                return Optional.of(fullText);
            }
            log.warn(
                    "종합 코멘트 검증 실패(시도 {}/{}): {} — {}",
                    attempt,
                    MAX_ATTEMPTS,
                    fullText,
                    result.violations());
        }

        log.warn("종합 코멘트 생성 {}회 모두 실패", MAX_ATTEMPTS);
        return Optional.empty();
    }

    // 모델이 지시를 어기고 마크다운 코드펜스(```json ... ```)로 감싸 내놓는 경우를 방어적으로 벗겨낸다.
    private List<String> parseParagraphs(String rawOutput) {
        try {
            String cleaned = rawOutput.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "").trim();
            }

            JsonNode root = mapper.readTree(cleaned);
            JsonNode paragraphsNode = root.get("paragraphs");
            if (paragraphsNode == null || !paragraphsNode.isArray() || paragraphsNode.isEmpty()) {
                return null;
            }

            List<String> paragraphs = new ArrayList<>();
            for (JsonNode paragraph : paragraphsNode) {
                paragraphs.add(paragraph.asText());
            }
            return paragraphs;
        } catch (Exception e) {
            return null;
        }
    }
}
