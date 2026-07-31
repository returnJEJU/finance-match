package com.financematch.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.common.ApiResponse;
import com.financematch.common.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 인증되지 않은 요청에 대한 401 응답을 {@link ApiResponse} 형식으로 직접 작성한다.
 *
 * <p>필터 단계에서 거절된 요청은 DispatcherServlet 까지 도달하지 못하므로 {@code
 * GlobalExceptionHandler}({@code @RestControllerAdvice})가 잡을 수 없다. 이 클래스가 없으면 스프링 시큐리티
 * 기본 동작(로그인 폼 리다이렉트·서블릿 컨테이너 기본 에러 페이지)이 나가고, 프론트의 공통 응답 파싱이 깨진다.
 *
 * <p>직접 호출하지 않는다. 인증이 필요한 경로에 인증 정보 없이 접근하면 시큐리티의 {@code
 * ExceptionTranslationFilter} 가 대신 호출한다. 인증은 됐지만 권한이 부족한 경우(403)는 {@code
 * AccessDeniedHandler} 가 담당하는 별도 흐름이다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        ErrorCode errorCode = resolveErrorCode(request);
        log.debug("인증 실패로 요청 거절: {} {} - {}", request.getMethod(), request.getRequestURI(),
                errorCode.name());

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 한글 메시지가 깨지지 않도록 인코딩을 명시한다. getWriter() 호출 전에 지정해야 반영된다.
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode));
    }

    /**
     * 401 의 사유를 정한다.
     *
     * <p>{@link JwtAuthenticationFilter} 가 토큰 검증에 실패하면 그 사유를 request 속성에 남긴다. 그 값이
     * 있으면 그대로 쓰고(만료·위조를 구분해 안내), 없으면 애초에 토큰을 보내지 않은 요청이므로 {@code
     * UNAUTHORIZED} 로 응답한다.
     */
    private ErrorCode resolveErrorCode(HttpServletRequest request) {
        Object authError = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR);

        return authError instanceof ErrorCode ? (ErrorCode) authError : ErrorCode.UNAUTHORIZED;
    }
}
