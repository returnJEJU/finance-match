package com.financematch.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.financematch.common.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * JWT 인증 필터 검증.
 *
 * <p>이 필터의 핵심 계약은 <b>"검증에 실패해도 요청을 막지 않는다"</b> 이다. 인증 정보를 채우지 않고
 * 실패 사유만 request 속성에 남긴 뒤 다음 필터로 넘긴다 — 차단 여부는 {@code SecurityConfig} 의 경로
 * 규칙이 정한다. 그래서 모든 케이스에서 "다음 필터로 넘어갔는가" 를 함께 확인한다.
 *
 * <p>{@code JwtProvider} 는 목이 아니라 실제 객체를 쓴다. 토큰 생성·검증은 이미
 * {@code JwtProviderTest} 가 덮고 있고, 여기서는 실제 토큰 문자열이 헤더를 통과하는 경로 전체를
 * 확인하는 편이 낫기 때문이다. 만료·위조 토큰도 유효기간과 비밀키를 달리한 provider 로 만든다.
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "test-secret-key-for-jwt-authentication-filter-0123456789";
    private static final String OTHER_SECRET =
            "another-secret-key-that-does-not-match-0123456789";
    private static final long ONE_HOUR_MS = 3_600_000L;
    private static final long ALREADY_EXPIRED_MS = -1_000L;
    private static final long REFRESH_VALIDITY_MS = 1_209_600_000L;

    private static final Long MEMBER_ID = 42L;

    private JwtProvider jwtProvider;
    private JwtAuthenticationFilter filter;

    /**
     * 블랙리스트는 Redis 를 쓰므로 목으로 둔다. 기본값(빈 목록)에서는 {@code contains} 가 false 라
     * 기존 케이스들은 그대로 동작하고, 폐기 케이스에서만 true 를 돌려주게 한다.
     */
    private TokenBlacklist tokenBlacklist;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, ONE_HOUR_MS, REFRESH_VALIDITY_MS);
        tokenBlacklist = mock(TokenBlacklist.class);
        filter = new JwtAuthenticationFilter(jwtProvider, tokenBlacklist);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        // SecurityContextHolder 는 ThreadLocal 이다. 비우지 않으면 인증 상태가 다음 테스트로 샌다.
        SecurityContextHolder.clearContext();
    }

    @Test
    void 토큰이_없으면_인증하지_않고_요청을_통과시킨다() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR));
        assertRequestPassedThrough();
    }

    @Test
    void Bearer_형식이_아닌_헤더는_토큰으로_보지_않는다() throws Exception {
        request.addHeader("Authorization", jwtProvider.createAccessToken(MEMBER_ID));

        filter.doFilter(request, response, filterChain);

        // 접두사가 없으면 토큰 자체를 꺼내지 않으므로 검증도, 실패 기록도 일어나지 않는다.
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR));
        assertRequestPassedThrough();
    }

    /**
     * 로그아웃한 토큰은 서명·만료가 멀쩡해도 통과시키지 않는다. JWT 는 발급 후 취소할 수 없으므로,
     * 폐기 목록을 매 요청 확인하는 것이 유일한 차단 수단이다.
     */
    @Test
    void 폐기된_토큰은_유효해도_인증하지_않는다() throws Exception {
        String token = jwtProvider.createAccessToken(MEMBER_ID);
        when(tokenBlacklist.contains(jwtProvider.getJti(token))).thenReturn(true);

        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(
                ErrorCode.INVALID_TOKEN, request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR));
        assertRequestPassedThrough();
    }

    /**
     * refresh 토큰을 access 자리에 쓰면 거절한다. 막지 않으면 2주짜리 토큰이 통행증이 되어 access 를
     * 짧게 유지하는 의미가 사라진다.
     */
    @Test
    void refresh_토큰으로는_인증되지_않는다() throws Exception {
        request.addHeader("Authorization", "Bearer " + jwtProvider.createRefreshToken(MEMBER_ID));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(
                ErrorCode.INVALID_TOKEN, request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR));
        assertRequestPassedThrough();
    }

    @Test
    void 정상_토큰이면_회원ID가_SecurityContext_에_저장된다() throws Exception {
        request.addHeader("Authorization", "Bearer " + jwtProvider.createAccessToken(MEMBER_ID));

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        // principal 이 Long 이어야 한다 — LoginMemberArgumentResolver 가 이 타입을 전제로 꺼내 쓴다.
        assertEquals(MEMBER_ID, authentication.getPrincipal());
        assertNull(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR));
        assertRequestPassedThrough();
    }

    @Test
    void 인증에_성공하면_요청_부가정보가_함께_기록된다() throws Exception {
        request.addHeader("Authorization", "Bearer " + jwtProvider.createAccessToken(MEMBER_ID));

        filter.doFilter(request, response, filterChain);

        // 감사 로그용 details(요청 IP·세션 ID). 빠지면 인증은 되지만 추적이 불가능해진다.
        assertNotNull(SecurityContextHolder.getContext().getAuthentication().getDetails());
    }

    @Test
    void Bearer_뒤에_붙은_공백은_제거하고_토큰을_읽는다() throws Exception {
        request.addHeader(
                "Authorization", "Bearer " + jwtProvider.createAccessToken(MEMBER_ID) + "  ");

        filter.doFilter(request, response, filterChain);

        assertEquals(MEMBER_ID, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void 만료된_토큰이면_EXPIRED_TOKEN_을_남기고_요청은_통과시킨다() throws Exception {
        String expiredToken =
                new JwtProvider(SECRET, ALREADY_EXPIRED_MS, REFRESH_VALIDITY_MS).createAccessToken(MEMBER_ID);
        request.addHeader("Authorization", "Bearer " + expiredToken);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(ErrorCode.EXPIRED_TOKEN, request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR));
        // 401 을 여기서 쓰지 않는다. 응답 작성은 JwtAuthenticationEntryPoint 의 몫이다.
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertRequestPassedThrough();
    }

    @Test
    void 위조된_토큰이면_INVALID_TOKEN_을_남기고_요청은_통과시킨다() throws Exception {
        String forgedToken =
                new JwtProvider(OTHER_SECRET, ONE_HOUR_MS, REFRESH_VALIDITY_MS).createAccessToken(MEMBER_ID);
        request.addHeader("Authorization", "Bearer " + forgedToken);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(ErrorCode.INVALID_TOKEN, request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR));
        assertRequestPassedThrough();
    }

    @Test
    void 토큰_형식이_아닌_문자열은_INVALID_TOKEN_으로_처리한다() throws Exception {
        request.addHeader("Authorization", "Bearer not-a-jwt");

        filter.doFilter(request, response, filterChain);

        assertEquals(ErrorCode.INVALID_TOKEN, request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR));
        assertRequestPassedThrough();
    }

    @Test
    void 검증에_실패하면_앞서_남아있던_인증정보를_지운다() throws Exception {
        // 같은 스레드에서 이전 요청의 인증이 남아 있는 상황을 만든다.
        JwtAuthenticationFilter first = new JwtAuthenticationFilter(jwtProvider, tokenBlacklist);
        MockHttpServletRequest authenticated = new MockHttpServletRequest();
        authenticated.addHeader("Authorization", "Bearer " + jwtProvider.createAccessToken(MEMBER_ID));
        first.doFilter(authenticated, new MockHttpServletResponse(), new MockFilterChain());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        request.addHeader("Authorization", "Bearer not-a-jwt");
        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /** 필터가 다음 단계로 요청을 넘겼는지. 넘겼다면 MockFilterChain 에 요청이 기록된다. */
    private void assertRequestPassedThrough() {
        assertNotNull(filterChain.getRequest(), "요청이 다음 필터로 전달되지 않았다");
    }
}
