package com.financematch.auth.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.auth.annotation.LoginMember;
import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@code @LoginMember} 파라미터 주입기 검증.
 *
 * <p>이 리졸버는 토큰을 직접 해석하지 않는다. {@link com.financematch.auth.jwt.JwtAuthenticationFilter}
 * 가 {@link SecurityContextHolder} 에 넣어둔 값을 꺼내 쓸 뿐이다. 따라서 검증 대상은 두 가지다 —
 * <b>어떤 파라미터를 맡을지</b>({@code supportsParameter}) 와 <b>인증 정보를 어떻게 해석할지</b>
 * ({@code resolveArgument}).
 *
 * <p>후자는 통과시키면 안 되는 경우를 빠짐없이 막는 것이 중요하다. 여기서 새면 인증되지 않은 요청이
 * 컨트롤러까지 도달한다.
 */
class LoginMemberArgumentResolverTest {

    private static final Long MEMBER_ID = 42L;

    private final LoginMemberArgumentResolver resolver = new LoginMemberArgumentResolver();

    @AfterEach
    void tearDown() {
        // SecurityContextHolder 는 ThreadLocal 이다. 비우지 않으면 인증 상태가 다음 테스트로 샌다.
        SecurityContextHolder.clearContext();
    }

    // ===== supportsParameter =====

    @Test
    void LoginMember_가_붙은_Long_파라미터를_맡는다() throws Exception {
        assertTrue(resolver.supportsParameter(parameterOf("annotatedLong", Long.class)));
    }

    @Test
    void LoginMember_가_붙어도_Long_이_아니면_맡지_않는다() throws Exception {
        // 타입까지 확인하지 않으면 String 파라미터에 Long 을 넣으려다 런타임에 터진다.
        assertFalse(resolver.supportsParameter(parameterOf("annotatedString", String.class)));
    }

    @Test
    void 애노테이션이_없는_Long_파라미터는_맡지_않는다() throws Exception {
        assertFalse(resolver.supportsParameter(parameterOf("plainLong", Long.class)));
    }

    // ===== resolveArgument =====

    @Test
    void 인증된_요청은_회원ID_를_돌려준다() throws Exception {
        setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        MEMBER_ID, null, AuthorityUtils.NO_AUTHORITIES));

        Object resolved = resolver.resolveArgument(
                parameterOf("annotatedLong", Long.class), null, null, null);

        assertEquals(MEMBER_ID, resolved);
    }

    @Test
    void 인증정보가_아예_없으면_UNAUTHORIZED_다() throws Exception {
        assertUnauthorized();
    }

    @Test
    void 익명_사용자는_UNAUTHORIZED_다() throws Exception {
        // 익명 토큰의 principal 은 문자열("anonymousUser") 이라 타입 검사에서 걸러진다.
        setAuthentication(
                new AnonymousAuthenticationToken(
                        "key",
                        "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertUnauthorized();
    }

    @Test
    void 인증되지_않은_토큰이_들어있으면_UNAUTHORIZED_다() throws Exception {
        // 권한 없이 만든 토큰은 isAuthenticated() 가 false 다.
        setAuthentication(new UsernamePasswordAuthenticationToken(MEMBER_ID, null));

        assertUnauthorized();
    }

    @Test
    void principal_이_Long_이_아니면_UNAUTHORIZED_다() throws Exception {
        // 필터가 넣는 principal 타입이 바뀌면 여기서 걸린다.
        setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "42", null, AuthorityUtils.NO_AUTHORITIES));

        assertUnauthorized();
    }

    // ===== 도우미 =====

    private void assertUnauthorized() throws Exception {
        MethodParameter parameter = parameterOf("annotatedLong", Long.class);

        ApiException e =
                assertThrows(
                        ApiException.class,
                        () -> resolver.resolveArgument(parameter, null, null, null));

        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
    }

    private void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private MethodParameter parameterOf(String methodName, Class<?> parameterType)
            throws NoSuchMethodException {
        return new MethodParameter(
                Handler.class.getDeclaredMethod(methodName, parameterType), 0);
    }

    /** {@code MethodParameter} 를 만들기 위한 컨트롤러 메서드 시그니처 표본. */
    @SuppressWarnings("unused")
    private static class Handler {
        void annotatedLong(@LoginMember Long memberId) {}

        void annotatedString(@LoginMember String memberId) {}

        void plainLong(Long memberId) {}
    }
}
