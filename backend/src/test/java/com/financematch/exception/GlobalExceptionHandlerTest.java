package com.financematch.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.auth.domain.Gender;
import com.financematch.common.ApiResponse;
import com.financematch.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 전역 예외 처리기 검증.
 *
 * <p>이 클래스의 값어치는 "예외를 잡는다" 가 아니라 <b>모든 실패가 같은 모양으로 나간다</b> 는 데 있다.
 * 프론트 공통 인터셉터가 {@code ApiResponse} 규격(`success`·`code`·`message`)에 맞춰 에러를 해석하므로,
 * 형식이 어긋나면 화면 전체의 에러 처리가 무너진다. 그래서 메서드를 직접 부르지 않고 <b>MockMvc 로 실제
 * 요청을 태워</b> 상태코드와 JSON 본문까지 확인한다.
 *
 * <p>검증 실패·본문 변환 실패는 손으로 만든 예외가 아니라 <b>실제 요청 처리 과정에서 스프링이 던지게</b>
 * 한다. 직접 만든 예외로 테스트하면 "이 예외가 정말 이 상황에서 나오는가" 는 확인되지 않는다.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new TestController())
                        .setControllerAdvice(handler)
                        .build();
    }

    // ===== 서비스가 의도적으로 던진 예외 =====

    @Test
    void ApiException_은_ErrorCode_의_상태와_이름으로_응답한다() throws Exception {
        JsonNode body = perform(get("/boom/api"), 404);

        assertFalse(body.get("success").asBoolean());
        assertEquals("MEMBER_NOT_FOUND", body.get("code").asText());
        assertEquals(ErrorCode.MEMBER_NOT_FOUND.getMessage(), body.get("message").asText());
        assertTrue(body.get("data").isNull());
    }

    @Test
    void ApiException_에_담긴_상세_메시지를_그대로_내려준다() throws Exception {
        // 기본 메시지 대신 상황을 덧붙인 메시지를 던진 경우, 그 메시지가 유지되어야 한다.
        JsonNode body = perform(get("/boom/api-custom"), 409);

        assertEquals("EMAIL_EXISTS", body.get("code").asText());
        assertEquals("이미 사용 중입니다: hong@kb.com", body.get("message").asText());
    }

    // ===== @Valid 검증 실패 =====

    @Test
    void 검증_실패는_첫_필드_에러_메시지를_내려준다() throws Exception {
        JsonNode body = perform(jsonPost("{\"gender\": \"F\"}"), 400);

        assertEquals("INVALID_INPUT", body.get("code").asText());
        // ErrorCode 의 기본 문구가 아니라 필드에 적어둔 안내가 나가야 사용자가 무엇을 고칠지 안다.
        assertEquals("이름을 입력해 주세요.", body.get("message").asText());
    }

    /**
     * {@code getFieldError()} 가 null 인 경우의 대비책 검증.
     *
     * <p>필드가 아닌 객체 전체 수준의 검증 실패(클래스 레벨 제약 등)에서는 필드 에러가 없을 수 있다.
     * MockMvc 로는 이 상황을 안정적으로 재현하기 어려워 이 케이스만 핸들러를 직접 호출한다.
     */
    @Test
    void 필드_에러가_없으면_기본_메시지로_되돌아간다() throws Exception {
        MethodArgumentNotValidException e =
                new MethodArgumentNotValidException(
                        new MethodParameter(
                                TestController.class.getDeclaredMethod("valid", Payload.class), 0),
                        new BeanPropertyBindingResult(new Payload(), "payload"));

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(e);

        assertEquals(ErrorCode.INVALID_INPUT.getStatus(), response.getStatusCode());
        assertEquals(ErrorCode.INVALID_INPUT.name(), response.getBody().getCode());
        assertEquals(ErrorCode.INVALID_INPUT.getMessage(), response.getBody().getMessage());
    }

    // ===== 본문 변환 실패 =====

    @Test
    void 깨진_JSON_은_INVALID_INPUT_으로_응답한다() throws Exception {
        JsonNode body = perform(jsonPost("{\"name\":"), 400);

        assertEquals("INVALID_INPUT", body.get("code").asText());
        assertEquals(ErrorCode.INVALID_INPUT.getMessage(), body.get("message").asText());
    }

    @Test
    void 없는_enum_값도_INVALID_INPUT_으로_응답한다() throws Exception {
        // enum 변환 실패는 검증(@Valid) 이전 단계인 역직렬화에서 터진다. 같은 400 으로 모아야
        // 프론트가 "입력값 문제" 로 동일하게 처리할 수 있다.
        JsonNode body = perform(jsonPost("{\"name\":\"홍길동\", \"gender\":\"X\"}"), 400);

        assertEquals("INVALID_INPUT", body.get("code").asText());
    }

    // ===== 예상하지 못한 예외 =====

    @Test
    void 예상하지_못한_예외는_500_INTERNAL_ERROR_로_응답한다() throws Exception {
        JsonNode body = perform(get("/boom/unexpected"), 500);

        assertEquals("INTERNAL_ERROR", body.get("code").asText());
        assertEquals(ErrorCode.INTERNAL_ERROR.getMessage(), body.get("message").asText());
    }

    @Test
    void 예상하지_못한_예외의_내부_메시지는_노출하지_않는다() throws Exception {
        // 스택트레이스·SQL·커넥션 정보 같은 내부 사정이 그대로 나가면 공격자에게 힌트가 된다.
        JsonNode body = perform(get("/boom/unexpected"), 500);

        assertEquals(ErrorCode.INTERNAL_ERROR.getMessage(), body.get("message").asText());
        assertFalse(body.toString().contains("커넥션 풀 고갈"));
    }

    // ===== 도우미 =====

    /**
     * 요청을 태우고 상태코드를 확인한 뒤 응답 본문을 JSON 으로 돌려준다.
     *
     * <p>{@code jsonPath} 대신 {@code ObjectMapper} 로 읽는다 — jsonPath 는 별도 의존성
     * (json-path)이 필요한데, 이 검증에는 그만한 이점이 없다.
     */
    private JsonNode perform(RequestBuilder request, int expectedStatus) throws Exception {
        String content =
                mockMvc.perform(request)
                        .andExpect(status().is(expectedStatus))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readTree(content);
    }

    private RequestBuilder jsonPost(String body) {
        return post("/boom/valid").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    // ===== 테스트용 컨트롤러 =====

    /** 각 예외 유형을 실제 요청 처리 과정에서 발생시키기 위한 표본 컨트롤러. */
    @RestController
    static class TestController {

        @GetMapping("/boom/api")
        void apiException() {
            throw new ApiException(ErrorCode.MEMBER_NOT_FOUND);
        }

        @GetMapping("/boom/api-custom")
        void apiExceptionWithMessage() {
            throw new ApiException(ErrorCode.EMAIL_EXISTS, "이미 사용 중입니다: hong@kb.com");
        }

        @GetMapping("/boom/unexpected")
        void unexpected() {
            throw new IllegalStateException("커넥션 풀 고갈 — 내부에만 남아야 하는 문구");
        }

        @PostMapping("/boom/valid")
        void valid(@Valid @RequestBody Payload payload) {}
    }

    @Getter
    @Setter
    static class Payload {

        @NotBlank(message = "이름을 입력해 주세요.")
        private String name;

        @NotNull(message = "성별을 선택해 주세요.")
        private Gender gender;
    }
}
