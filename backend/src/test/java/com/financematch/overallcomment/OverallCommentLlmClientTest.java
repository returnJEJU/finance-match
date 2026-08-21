package com.financematch.overallcomment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 실제 OpenAI 호출 없이 내부 {@link HttpClient}를 mock으로 갈아끼워 generateComment()의 성공·실패
 * 분기를 검증한다.
 */
class OverallCommentLlmClientTest {

    private OverallCommentLlmClient client;
    private HttpClient mockHttp;

    @BeforeEach
    void setUp() {
        client = new OverallCommentLlmClient("test-api-key", "test-model");
        mockHttp = mock(HttpClient.class);
        ReflectionTestUtils.setField(client, "http", mockHttp);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }

    private void stubSend(HttpResponse<String> response) throws IOException, InterruptedException {
        when(mockHttp.<String>send(any(HttpRequest.class), any())).thenReturn(response);
    }

    @Test
    void 정상_응답이면_text를_trim해서_반환한다() throws Exception {
        String body =
                """
                {
                  "output": [
                    {"type": "reasoning"},
                    {"type": "message", "content": [{"text": "  안녕하세요  "}]}
                  ]
                }
                """;
        stubSend(mockResponse(200, body));

        String result = client.generateComment("프롬프트");

        assertEquals("안녕하세요", result);
    }

    @Test
    void 상태코드가_200이_아니면_예외() throws Exception {
        stubSend(mockResponse(500, "에러 본문"));

        OverallCommentLlmClient.LlmCallException ex =
                assertThrows(
                        OverallCommentLlmClient.LlmCallException.class,
                        () -> client.generateComment("프롬프트"));
        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    @Test
    void output_필드가_없으면_예외() throws Exception {
        stubSend(mockResponse(200, "{}"));

        assertThrows(
                OverallCommentLlmClient.LlmCallException.class, () -> client.generateComment("프롬프트"));
    }

    @Test
    void output이_빈배열이면_예외() throws Exception {
        stubSend(mockResponse(200, "{\"output\": []}"));

        assertThrows(
                OverallCommentLlmClient.LlmCallException.class, () -> client.generateComment("프롬프트"));
    }

    @Test
    void message_타입_항목이_없으면_예외() throws Exception {
        stubSend(mockResponse(200, "{\"output\": [{\"type\": \"reasoning\"}]}"));

        assertThrows(
                OverallCommentLlmClient.LlmCallException.class, () -> client.generateComment("프롬프트"));
    }

    @Test
    void content_필드가_없으면_예외() throws Exception {
        stubSend(mockResponse(200, "{\"output\": [{\"type\": \"message\"}]}"));

        assertThrows(
                OverallCommentLlmClient.LlmCallException.class, () -> client.generateComment("프롬프트"));
    }

    @Test
    void content가_빈배열이면_예외() throws Exception {
        stubSend(mockResponse(200, "{\"output\": [{\"type\": \"message\", \"content\": []}]}"));

        assertThrows(
                OverallCommentLlmClient.LlmCallException.class, () -> client.generateComment("프롬프트"));
    }

    @Test
    void text_필드가_없으면_예외() throws Exception {
        stubSend(mockResponse(200, "{\"output\": [{\"type\": \"message\", \"content\": [{}]}]}"));

        assertThrows(
                OverallCommentLlmClient.LlmCallException.class, () -> client.generateComment("프롬프트"));
    }

    @Test
    void IO예외가_발생하면_LlmCallException으로_감싼다() throws Exception {
        when(mockHttp.<String>send(any(HttpRequest.class), any())).thenThrow(new IOException("연결 실패"));

        OverallCommentLlmClient.LlmCallException ex =
                assertThrows(
                        OverallCommentLlmClient.LlmCallException.class,
                        () -> client.generateComment("프롬프트"));
        assertInstanceOf(IOException.class, ex.getCause());
    }

    @Test
    void 인터럽트가_발생하면_LlmCallException으로_감싸고_인터럽트_상태를_복구한다() throws Exception {
        when(mockHttp.<String>send(any(HttpRequest.class), any()))
                .thenThrow(new InterruptedException("중단"));

        try {
            assertThrows(
                    OverallCommentLlmClient.LlmCallException.class,
                    () -> client.generateComment("프롬프트"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted(); // 다른 테스트에 영향 주지 않도록 인터럽트 상태 정리
        }
    }

    /**
     * 응답 스키마 파싱 실패 처리.
     *
     * <p>실제 인자는 클래스 안의 상수라 운영에서는 실패할 수 없다. 다만 {@code readTree} 가 검사
     * 예외를 던져 처리를 생략할 수 없고, 생성자 안에 두면 그 처리가 <b>영원히 실행되지 않는 채로</b>
     * 남는다. 파싱을 인자 받는 메서드로 분리해 두었으므로 여기서 고정한다.
     */
    @Test
    void 스키마가_JSON_이_아니면_기동을_멈춘다() {
        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> client.parseSchema("{\"type\":"));

        assertEquals("응답 스키마 파싱 실패", e.getMessage());
        assertInstanceOf(IOException.class, e.getCause());
    }

    @Test
    void 정상_스키마는_그대로_파싱된다() {
        assertEquals("object", client.parseSchema("{\"type\":\"object\"}").get("type").asText());
    }
}
