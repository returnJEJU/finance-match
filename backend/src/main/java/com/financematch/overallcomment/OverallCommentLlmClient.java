package com.financematch.overallcomment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenAI Responses API 호출 래퍼. 참고 프로젝트(scratchpad의 java-openai)와 동일하게 Structured
 * Outputs(json_schema, strict)를 써서 모델이 항상 스키마에 맞는 형태로만 답하게 강제한다 —
 * {@link #generateComment}는 {@code {"paragraphs": [...]}}, {@link #checkTone}은 {@code
 * {"valid": boolean, "violations": [...]}}. 두 호출 모두 요청 조립·응답 파싱 로직은 {@link
 * #callResponsesApi}를 공유하고 스키마만 다르다.
 *
 * <p>처음엔 Chat Completions로 만들었는데, 모델(gpt-5-nano)이 문단을 나열할 때
 * {@code "paragraphs": ["a"], ["b"]} 처럼 배열 문법을 어겨 파싱 실패가 반복됐다(실제 호출로
 * 확인). json_schema strict 모드는 응답 자체를 스키마에 맞게 생성하도록 모델에 제약을 걸기 때문에
 * 이 실패 유형이 원천적으로 나오지 않는다.
 */
@Slf4j
@Component
public class OverallCommentLlmClient {

    private static final String ENDPOINT = "https://api.openai.com/v1/responses";

    private static final String PARAGRAPHS_SCHEMA =
            """
            {
              "type": "json_schema",
              "name": "comment_output",
              "schema": {
                "type": "object",
                "properties": {
                  "paragraphs": { "type": "array", "items": { "type": "string" } }
                },
                "required": ["paragraphs"],
                "additionalProperties": false
              },
              "strict": true
            }
            """;

    // axisChecks를 valid보다 앞에 둔 게 핵심이다(2026-08-25) — json_schema strict 모드는 객체
    // 필드를 선언 순서대로 채우므로, 모델이 축마다 "어느 문장을 근거로 봤는지"부터 인용하게 강제한
    // 뒤에야 최종 valid/violations를 채우게 된다. 원래는 valid/violations만 있어서 모델이 근거 없이
    // 바로 결론부터 냈는데, 골든셋 실험(OverallCommentToneValidatorGoldenSetTest)에서 그게
    // FPR 66.7~88.9%의 원인 중 하나로 보였다 — 특히 약점 축이 여러 개일 때 "이 중 하나라도 보완
    // 필요 톤이 있는지" 스캔을 근거 없이 하다 보니 있는데도 없다고 오판하는 경우가 잦았다.
    // axisChecks로 축별 인용을 먼저 시키면 이 스캔 자체가 명시적인 단계가 된다.
    private static final String TONE_CHECK_SCHEMA =
            """
            {
              "type": "json_schema",
              "name": "tone_check_output",
              "schema": {
                "type": "object",
                "properties": {
                  "axisChecks": {
                    "type": "array",
                    "items": {
                      "type": "object",
                      "properties": {
                        "axis": { "type": "string" },
                        "quote": { "type": "string" },
                        "toneOk": { "type": "boolean" }
                      },
                      "required": ["axis", "quote", "toneOk"],
                      "additionalProperties": false
                    }
                  },
                  "valid": { "type": "boolean" },
                  "violations": { "type": "array", "items": { "type": "string" } }
                },
                "required": ["axisChecks", "valid", "violations"],
                "additionalProperties": false
              },
              "strict": true
            }
            """;

    private final String apiKey;
    private final String model;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonNode formatSchema;
    private final JsonNode toneCheckSchema;

    public OverallCommentLlmClient(
            @Value("${openai.api.key}") String apiKey, @Value("${openai.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.formatSchema = parseSchema(PARAGRAPHS_SCHEMA);
        this.toneCheckSchema = parseSchema(TONE_CHECK_SCHEMA);
    }

    /**
     * 응답 스키마 JSON 을 파싱한다.
     *
     * <p>인자로 받는 이유는 하나다 — 파싱 실패 처리를 검증할 수 있게 하기 위함이다. 실제 인자는
     * 상수({@code PARAGRAPHS_SCHEMA})라 운영에서는 실패할 수 없지만, {@code readTree} 가 검사 예외를
     * 던져 처리를 생략할 수 없다. 생성자 안에 두면 그 처리가 영원히 실행되지 않는 채로 남는다.
     */
    JsonNode parseSchema(String schemaJson) {
        try {
            return mapper.readTree(schemaJson);
        } catch (IOException e) {
            throw new IllegalStateException("응답 스키마 파싱 실패", e);
        }
    }

    /** 프롬프트 문자열을 넣으면 {@code {"paragraphs": [...]}} 형태의 JSON 문자열을 그대로 반환한다. */
    public String generateComment(String prompt) {
        return callResponsesApi(prompt, formatSchema, "종합코멘트");
    }

    /**
     * 생성된 코멘트를 다시 프롬프트에 넣어 강점/약점 문단의 톤이 규칙과 맞는지 LLM에게 판정시킨다.
     * 정규식으로는 "이 문장이 긍정인지 부정인지"(의미) 판단이 안 되기 때문에({@link
     * OverallCommentRuleValidator}·{@link OverallCommentValidator}는 형식만 검사) 별도 LLM 호출로
     * 뗀 것이다. {@code {"valid": boolean, "violations": [...]}} 형태의 JSON 문자열을 그대로 반환한다.
     */
    public String checkTone(String prompt) {
        return callResponsesApi(prompt, toneCheckSchema, "톤검증");
    }

    private String callResponsesApi(String prompt, JsonNode schema, String logLabel) {
        long startedAt = System.currentTimeMillis();
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("input", prompt);
            ObjectNode text = mapper.createObjectNode();
            text.set("format", schema);
            body.set("text", text);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(ENDPOINT))
                            .timeout(Duration.ofSeconds(90))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + apiKey)
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                            .build();

            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new LlmCallException(
                        "OpenAI API 호출 실패 (HTTP " + response.statusCode() + "): " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode output = root.path("output");
            if (!output.isArray() || output.isEmpty()) {
                throw new LlmCallException("OpenAI 응답에 output이 없습니다: " + response.body());
            }
            // gpt-5 계열(reasoning 모델)은 output[0]이 내용 없는 "reasoning" 항목이고, 실제 답변은
            // 그 뒤의 "message" 타입 항목에 들어있다. 위치가 아니라 type으로 찾아야 한다.
            JsonNode messageItem = null;
            for (JsonNode item : output) {
                if ("message".equals(item.path("type").asText())) {
                    messageItem = item;
                    break;
                }
            }
            if (messageItem == null) {
                throw new LlmCallException("OpenAI 응답에 message 항목이 없습니다: " + response.body());
            }
            JsonNode content = messageItem.path("content");
            if (!content.isArray() || content.isEmpty()) {
                throw new LlmCallException("OpenAI 응답에 content가 없습니다: " + response.body());
            }
            JsonNode textNode = content.get(0).path("text");
            if (textNode.isMissingNode()) {
                throw new LlmCallException("OpenAI 응답에서 text 를 찾을 수 없음: " + response.body());
            }
            return textNode.asText().trim();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new LlmCallException("OpenAI 호출 중 오류", e);
        } finally {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.info("[LLM-TIMING] {} 호출 소요 {}ms", logLabel, elapsedMs);
        }
    }

    public static class LlmCallException extends RuntimeException {
        public LlmCallException(String message) {
            super(message);
        }

        public LlmCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
