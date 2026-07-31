package com.financematch.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * 스프링 시큐리티 필터체인을 서블릿 컨테이너에 등록한다.
 *
 * <p>{@code SecurityConfig} 가 만드는 필터체인은 스프링 빈이라 톰캣이 알지 못한다. 부모 클래스가 톰캣에
 * {@code DelegatingFilterProxy} 를 {@code /*} 로 등록해 주고, 그 프록시가 요청마다 루트 컨텍스트에서
 * {@code springSecurityFilterChain} 빈을 찾아 실제 처리를 위임한다. 톰캣이 만든 필터에는 스프링이 의존성을
 * 주입할 수 없으므로, 껍데기만 컨테이너에 등록하고 알맹이는 스프링이 관리하는 구조다.
 *
 * <p>몸통이 비어 있는 것이 정상이다. {@code onStartup()} 은 부모에서 {@code final} 이라 재정의할 수 없고,
 * 상속만으로 등록이 끝난다. 루트 컨텍스트는 {@code WebAppInitializer} 가 이미 만들고 있으므로 설정 클래스를
 * 넘기는 생성자는 쓰지 않는다 — 넘기면 컨텍스트가 중복 생성된다.
 *
 * <p>이 클래스는 어디에서도 참조되지 않는다. 서블릿 3.0 의 {@code ServletContainerInitializer} 가
 * {@code WebApplicationInitializer} 구현체를 자동으로 찾아 실행하는 방식이며, {@code WebAppInitializer} 가
 * DispatcherServlet 을 등록하는 것과 같은 경로다. <b>빠뜨려도 컴파일·기동 오류가 나지 않고 보안만 적용되지
 * 않으므로 지우지 말 것.</b>
 */
public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {}
