package com.financematch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.auth.jwt.JwtAuthenticationEntryPoint;
import com.financematch.auth.jwt.JwtAuthenticationFilter;
import com.financematch.auth.jwt.JwtProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * 보안 설정: 비밀번호 인코더와 JWT 기반 필터체인.
 *
 * <p>이 설정은 루트 컨텍스트에 등록된다({@code WebAppInitializer}). {@code @EnableWebSecurity} 가 만드는
 * {@code springSecurityFilterChain} 빈을 톰캣이 이름으로 찾아가야 하는데, 그 조회 대상이 루트 컨텍스트이기
 * 때문이다. 실제 등록은 {@code SecurityWebApplicationInitializer} 가 한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 비밀번호 해시 인코더.
     *
     * <p>BCrypt 는 같은 비밀번호라도 매번 다른 해시를 만든다(해시 안에 salt 가 포함된다). 그래서 저장된
     * 해시를 되돌려 비교하는 것이 아니라 {@code matches(평문, 해시)} 로 검증한다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtProvider jwtProvider) {
        return new JwtAuthenticationFilter(jwtProvider);
    }

    /**
     * 401 응답 작성기.
     *
     * <p>{@code ObjectMapper} 를 여기서 새로 만든다. {@code WebConfig} 에 이미 있지만 그것은 자식인 서블릿
     * 컨텍스트의 빈이라 부모인 루트 컨텍스트에서는 참조할 수 없다. {@code ApiResponse} 는 문자열·boolean 만
     * 담아 날짜 직렬화 같은 추가 설정이 필요 없으므로 기본 설정으로 충분하다.
     */
    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint(new ObjectMapper());
    }

    /**
     * JWT 기반 무상태 필터체인.
     *
     * <p><b>경로는 {@code /api} 를 포함해서 쓴다.</b> 시큐리티 필터는 DispatcherServlet 앞단, 즉 어떤
     * 서블릿이 처리할지 정해지기 전에 동작하므로 요청의 전체 경로를 본다. 컨트롤러가 {@code /v1/auth} 로 쓰는
     * 것은 DispatcherServlet 이 {@code /api} 를 떼고 넘겨주기 때문이며, 여기서 {@code /v1/...} 로 쓰면
     * 규칙이 어디에도 걸리지 않는다.
     *
     * <p><b>모든 경로를 {@code permitAll} 로 둔다.</b> 팀원들이 아직 임시 회원 ID 로 개발 중이라 지금
     * 잠그면 개발이 막힌다. {@code permitAll} 은 차단만 하지 않는다는 뜻이고 JWT 필터는 모든 요청에서 그대로
     * 동작하므로, 토큰을 보낸 요청은 {@code @LoginMember} 가 동작하고 나머지는 종전과 같이 동작한다. 팀원
     * 작업이 정리되면 {@code anyRequest().authenticated()} 로 바꾼다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint)
            throws Exception {

        return http
                // CSRF 는 브라우저가 쿠키를 자동 전송하는 것을 악용하는 공격이다. 인증 정보를
                // Authorization 헤더로 받으면 성립하지 않는다. 켜두면 모든 POST 가 403 이 된다.
                .csrf()
                .disable()
                // 로그인 화면은 Vue 에 있고 인증은 /api/v1/auth/login 이 담당한다.
                .formLogin()
                .disable()
                // 브라우저 기본 인증 팝업을 띄우지 않는다.
                .httpBasic()
                .disable()
                // JWT 가 요청마다 회원 정보를 실어 오므로 서버는 세션을 만들지 않는다.
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 필터 단계에서 거절된 요청은 GlobalExceptionHandler 가 잡지 못하므로 여기서 처리한다.
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .and()
                .authorizeHttpRequests()
                // 인증 없이 열려야 하는 경로. 로그인해야 토큰을 받으므로 인증을 걸 수 없다.
                .requestMatchers(
                        new AntPathRequestMatcher("/api/v1/auth/**"),
                        new AntPathRequestMatcher("/api/health"))
                .permitAll()
                // CORS preflight 에는 브라우저가 Authorization 헤더를 붙이지 않는다. 막으면 프론트의
                // 모든 요청이 CORS 오류로 실패한다.
                .requestMatchers(new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name()))
                .permitAll()
                .anyRequest()
                .permitAll()
                .and()
                // 인증 판정 자리 바로 앞에서 SecurityContext 를 채운다. formLogin 을 껐으므로 기준으로 쓴
                // 필터 자체는 체인에 없고, 순서상의 위치 표지로만 쓰인다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
