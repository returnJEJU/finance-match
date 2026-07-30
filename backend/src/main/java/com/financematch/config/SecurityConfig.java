package com.financematch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 보안 설정.
 *
 * <p>지금은 비밀번호 해시 인코더만 등록한다. 필터체인({@code @EnableWebSecurity})은 로그인·JWT 검증을
 * 만드는 단계에서 추가한다 — 토큰을 발급받을 로그인 API 가 없는 상태로 켜면 모든 API 가 막힌다.
 */
@Configuration
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
}
