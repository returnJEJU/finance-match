package com.financematch.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

/**
 * 인증 실패(401) 응답 작성 검증.
 *
 * <p>필터 단계에서 거절된 요청은 DispatcherServlet 까지 도달하지 못해 {@code GlobalExceptionHandler}
 * 가 잡을 수 없다. 그래서 이 클래스가 {@code ApiResponse} 형식을 직접 만든다 — 형식이 어긋나면 프론트의
 * 공통 응답 파싱이 깨지므로, 상태코드·본문 형식·인코딩을 모두 확인한다.
 *
 * <p>사유는 {@link JwtAuthenticationFilter} 가 request 속성에 남긴 값을 읽어 정한다. 그 값이 있으면
 * 만료·위조를 구분해 안내하고, 없으면 애초에 토큰을 보내지 않은 요청이므로 {@code UNAUTHORIZED} 다.
 */
class JwtAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAuthenticationEntryPoint entryPoint =
            new JwtAuthenticationEntryPoint(objectMapper);

    private final AuthenticationException authException =
            new InsufficientAuthenticationException("인증이 필요합니다");

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/api/v1/members/me");
        response = new MockHttpServletResponse();
    }

    @Test
    void 토큰을_보내지_않은_요청은_UNAUTHORIZED_로_응답한다() throws Exception {
        entryPoint.commence(request, response, authException);

        assertErrorResponse(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 필터가_남긴_만료_사유를_그대로_응답한다() throws Exception {
        request.setAttribute(JwtAuthenticationFilter.AUTH_ERROR, ErrorCode.EXPIRED_TOKEN);

        entryPoint.commence(request, response, authException);

        assertErrorResponse(ErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void 필터가_남긴_위조_사유를_그대로_응답한다() throws Exception {
        request.setAttribute(JwtAuthenticationFilter.AUTH_ERROR, ErrorCode.INVALID_TOKEN);

        entryPoint.commence(request, response, authException);

        assertErrorResponse(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void 속성에_ErrorCode_가_아닌_값이_있으면_UNAUTHORIZED_로_되돌아간다() throws Exception {
        // 다른 코드가 같은 이름의 속성을 덮어써도 401 응답 형식은 유지되어야 한다.
        request.setAttribute(JwtAuthenticationFilter.AUTH_ERROR, "EXPIRED_TOKEN");

        entryPoint.commence(request, response, authException);

        assertErrorResponse(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 응답은_JSON_이고_한글_메시지가_깨지지_않는다() throws Exception {
        request.setAttribute(JwtAuthenticationFilter.AUTH_ERROR, ErrorCode.EXPIRED_TOKEN);

        entryPoint.commence(request, response, authException);

        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding());
        // 인코딩 지정이 getWriter() 뒤로 밀리면 여기서 깨진 문자가 나온다.
        assertTrue(response.getContentAsString(StandardCharsets.UTF_8).contains("만료된 토큰입니다."));
    }

    /** 401 상태코드와 {@code ApiResponse.fail} 형식(success·code·message)을 함께 확인한다. */
    private void assertErrorResponse(ErrorCode expected) throws Exception {
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());

        JsonNode body = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
        assertFalse(body.get("success").asBoolean());
        assertEquals(expected.name(), body.get("code").asText());
        assertEquals(expected.getMessage(), body.get("message").asText());
        assertTrue(body.get("data").isNull());
    }
}
