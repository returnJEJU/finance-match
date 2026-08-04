package com.financematch.report.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenAI Chat Completions 호출 래퍼. 프롬프트 문자열 하나를 넣으면 모델 응답 텍스트 하나를 반환한다.
 *
 * <p>report_llm_case_practice 프로젝트의 {@code OpenAiClient}와 동일한 역할 — SDK 없이 순수
 * {@code HttpClient} + Jackson으로 직접 호출한다. 그 프로젝트에서 gpt-4o-mini로 검증된 방식을
 * 그대로 가져왔고, 기본 모델만 gpt-5-nano로 바꿨다.
 *
 * <p>주의: gpt-5 계열은 chat completions 파라미터 지원 범위가 gpt-4o 계열과 다를 수 있다(예:
 * temperature 미지원 가능성). 실제 키로 첫 호출 테스트 시 확인이 필요하다.
 */
@Slf4j
@Component
public class ReportLlmClient {

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportLlmClient(
            @Value("${openai.api.key}") String apiKey, @Value("${openai.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    // axisLabel 은 호출부(부채·투자가치관·절세 ReasonService)를 구분해 소요 시간을 로그로 남기기 위한 값이다.
    public String generateReason(String axisLabel, String prompt) {
        // gpt-5 계열(gpt-5-nano 포함)은 기본값(1) 외의 temperature 를 거부한다(HTTP 400) —
        // 모델 종류를 가리지 않는 공통 클라이언트이므로 temperature 는 아예 지정하지 않는다.
        Map<String, Object> body =
                Map.of(
                        "model", model,
                        "messages", List.of(Map.of("role", "user", "content", prompt)));

        long startedAt = System.currentTimeMillis();
        try {
            String requestJson = mapper.writeValueAsString(body);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(ENDPOINT))
                            .timeout(Duration.ofSeconds(90))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + apiKey)
                            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                            .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new LlmCallException(
                        "OpenAI API 호출 실패 (HTTP " + response.statusCode() + "): " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode()) {
                throw new LlmCallException("응답에서 content 를 찾을 수 없음: " + response.body());
            }
            return content.asText().trim();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new LlmCallException("OpenAI 호출 중 오류", e);
        } finally {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.info("[LLM-TIMING] {} 축 호출 소요 {}ms", axisLabel, elapsedMs);
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
