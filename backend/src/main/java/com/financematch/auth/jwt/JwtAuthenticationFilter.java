package com.financematch.auth.jwt;

import com.financematch.exception.ApiException;
import java.io.IOException;
import java.util.Collections;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer <token>} 헤더의 JWT 를 검증해 {@link SecurityContextHolder} 에 인증 정보를
 * 저장하는 필터.
 *
 * <p>필터는 DispatcherServlet 앞단에서 동작한다. 즉 컨트롤러가 호출되기 전에 "이 요청은 몇 번 회원인가"를
 * 확정해두는 역할이다. {@code @LoginMember} 리졸버는 토큰을 다시 해석하지 않고 여기서 저장한 값을 꺼내 쓴다.
 *
 * <p><b>토큰이 없거나 검증에 실패해도 요청을 막지 않는다.</b> 인증 정보를 채우지 않은 채 다음 필터로 넘길
 * 뿐이다. 차단 여부는 {@code SecurityConfig} 의 경로 규칙이 결정한다 — 검증(이 필터)과 인가(설정)를 분리하면
 * 인증이 필요한 경로를 늘리거나 줄일 때 필터를 건드리지 않아도 된다.
 *
 * <p>실패 사유는 {@link #AUTH_ERROR} 속성으로만 남긴다. 필터에서 던진 예외는 {@code
 * GlobalExceptionHandler}({@code @RestControllerAdvice})가 잡지 못하기 때문이다 — 예외 처리기는
 * DispatcherServlet 안쪽에서만 동작한다. 이 속성은 {@link JwtAuthenticationEntryPoint} 가 읽어 401 응답의
 * 에러 코드로 사용한다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 토큰 검증 실패 사유({@code ErrorCode})를 담아두는 request 속성 이름. */
    public static final String AUTH_ERROR = "authError";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                Long memberId = jwtProvider.getMemberId(token);
                authenticate(request, memberId);

                // JwtProvider 가 만료·위조를 ApiException(EXPIRED_TOKEN·INVALID_TOKEN)으로 바꿔 던진다.
                // (ExpiredJwtException 이 JwtException 의 하위 타입이라는 catch 순서 문제도 거기서 처리됨)
            } catch (ApiException e) {
                // 인증만 실패시키고 요청은 그대로 통과시킨다. 여기서 응답을 쓰지 않는다.
                SecurityContextHolder.clearContext();
                request.setAttribute(AUTH_ERROR, e.getErrorCode());
                log.debug("JWT 인증 실패: {}", e.getErrorCode().name());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * {@code Authorization} 헤더에서 토큰 문자열만 꺼낸다.
     *
     * @return 헤더가 없거나 {@code Bearer } 형식이 아니면 {@code null}
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * 검증된 회원 ID 를 인증 정보로 만들어 {@link SecurityContextHolder} 에 저장한다.
     *
     * <p>{@code principal} 에 회원 ID({@code Long})를 넣는 것이 핵심이다. {@code
     * LoginMemberArgumentResolver} 가 {@code principal instanceof Long} 으로 검사하므로 이 타입이 어긋나면
     * 토큰이 유효해도 401 이 된다.
     *
     * <p>권한(authorities)은 빈 목록이다. 아직 역할 구분이 없고, 3-인자 생성자는 "이미 인증됨" 상태로
     * 만들어주므로 {@code isAuthenticated()} 는 true 가 된다.
     */
    private void authenticate(HttpServletRequest request, Long memberId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        memberId, null, Collections.emptyList());

        // 요청 IP·세션 ID 등 부가 정보. 감사 로그를 남길 때 쓰인다.
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
