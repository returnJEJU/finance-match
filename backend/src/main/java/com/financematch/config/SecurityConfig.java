package com.financematch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.auth.jwt.JwtAuthenticationEntryPoint;
import com.financematch.auth.jwt.JwtAuthenticationFilter;
import com.financematch.auth.jwt.JwtProvider;
import com.financematch.auth.jwt.TokenBlacklist;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtProvider jwtProvider, TokenBlacklist tokenBlacklist) {
        return new JwtAuthenticationFilter(jwtProvider, tokenBlacklist);
    }

    /**
     * 시큐리티 필터체인용 CORS 설정.
     *
     * <p>{@code WebConfig} 에도 CORS 설정이 있지만 그것은 MVC 레벨이라 DispatcherServlet 까지 도달한
     * 요청에만 적용된다. 필터가 401 로 막은 응답에는 붙지 않으므로 여기에 하나 더 둔다.
     *
     * <p>두 곳에서 헤더가 겹치지는 않는다 — 스프링의 {@code DefaultCorsProcessor} 는 응답에 이미
     * {@code Access-Control-Allow-Origin} 이 있으면 건너뛴다.
     *
     * <p>설정값은 {@code WebConfig} 와 <b>같은 프로퍼티</b>를 읽는다. 값을 따로 적으면 한쪽만 바꿨을 때
     * "어떤 응답은 되고 어떤 응답은 안 되는" 상태가 된다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(Environment env) {
        String origins = env.getProperty("cors.allowed-origins", "http://localhost:5173");

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(origins.split(",")));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
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
     * <p><b>기본값은 잠김({@code anyRequest().authenticated()})이다.</b> 열어야 하는 경로만 위쪽에
     * 명시적으로 나열한다 — 그래야 {@code @LoginMember} 를 빠뜨린 새 API 가 생겨도 열린 채로 배포되지
     * 않는다.
     *
     * <p>열어둔 경로는 세 종류다. ①{@code /api/v1/auth/**} — 로그인해야 토큰을 받으므로 인증을 걸 수
     * 없다. ②{@code /api/health} — 로드밸런서·모니터링용. ③{@code /swagger-ui/**}·
     * {@code /openapi.yaml} — API 문서. 시큐리티 필터가 {@code /*} 전체에 걸려 있어 여기를 열지 않으면
     * 정적 파일까지 401 이 되어 문서 화면이 뜨지 않는다.
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
                // 필터가 거절한 응답에도 CORS 헤더가 붙게 한다. WebConfig 의 CORS 설정은 MVC 레벨이라
                // DispatcherServlet 까지 도달한 요청에만 적용된다 — 필터가 401 로 막으면 거기까지 가지
                // 못해 헤더가 빠지고, 브라우저는 응답 본문을 프론트에 넘기지 않는다. 그러면 프론트가
                // EXPIRED_TOKEN 을 읽지 못해 "만료 시 로그인 화면으로" 분기가 동작하지 않는다.
                .cors()
                .and()
                .authorizeHttpRequests()
                // 인증 없이 열려야 하는 경로. 로그인해야 토큰을 받으므로 인증을 걸 수 없다.
                .requestMatchers(
                        new AntPathRequestMatcher("/api/v1/auth/**"),
                        new AntPathRequestMatcher("/api/health"))
                .permitAll()
                // API 문서. 시큐리티 필터는 /* 전체에 걸려 있어 여기를 열지 않으면 문서도 401 이 된다.
                .requestMatchers(
                        new AntPathRequestMatcher("/swagger-ui/**"),
                        new AntPathRequestMatcher("/openapi.yaml"))
                .permitAll()
                // CORS preflight 에는 브라우저가 Authorization 헤더를 붙이지 않는다. 막으면 프론트의
                // 모든 요청이 CORS 오류로 실패한다.
                .requestMatchers(new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name()))
                .permitAll()
                // 기본값을 "잠김"으로 둔다. 앞으로 @LoginMember 를 빠뜨린 새 API 가 생겨도 열린 채로
                // 배포되지 않는다. 열어야 하는 경로는 위에 명시적으로 추가한다.
                .anyRequest()
                .authenticated()
                .and()
                // 인증 판정 자리 바로 앞에서 SecurityContext 를 채운다. formLogin 을 껐으므로 기준으로 쓴
                // 필터 자체는 체인에 없고, 순서상의 위치 표지로만 쓰인다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
