package com.financematch.auth.resolver;

import com.financematch.auth.annotation.LoginMember;
import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link LoginMember} 가 붙은 파라미터에 인증된 회원 ID 를 주입한다.
 *
 * <p>이 리졸버는 토큰을 직접 해석하지 않는다. JWT 인증 필터가 {@link SecurityContextHolder} 에 넣어둔
 * 인증 정보를 꺼내 쓸 뿐이다. 따라서 필터가 적용되기 전에는 항상 {@code UNAUTHORIZED}(401) 가 된다.
 *
 * <p>{@code principal} 에는 회원 ID({@code Long}) 가 담긴다. 이 형태를 JWT 인증 필터와 맞춰야 한다.
 */
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    /** 처리 대상: {@code @LoginMember} 가 붙은 {@code Long} 파라미터. */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        // 익명 사용자의 principal 은 문자열("anonymousUser")이므로 타입 검사로 함께 걸러진다.
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        return principal;
    }
}
