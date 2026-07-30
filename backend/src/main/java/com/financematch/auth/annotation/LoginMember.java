package com.financematch.auth.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증된 회원의 ID 를 컨트롤러 메서드 파라미터로 주입받는다.
 *
 * <p>이 어노테이션 자체는 아무 동작도 하지 않는 표시일 뿐이다. 실제로 값을 채우는 것은 {@link
 * com.financematch.auth.resolver.LoginMemberArgumentResolver} 이고, 그 리졸버가 {@code WebConfig}
 * 에 등록되어 있어야 동작한다.
 *
 * <pre>
 * &#64;GetMapping("")
 * public ApiResponse&lt;ReportResponse&gt; getReport(&#64;LoginMember Long memberId) { ... }
 * </pre>
 *
 * <p>파라미터 타입은 {@code Long} 이어야 한다. 인증되지 않은 요청이면 리졸버가 {@code UNAUTHORIZED}(401)
 * 로 처리한다.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {}
