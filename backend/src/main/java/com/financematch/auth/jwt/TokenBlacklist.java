package com.financematch.auth.jwt;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 로그아웃한 access 토큰 목록 (Redis).
 *
 * <p><b>왜 필요한가.</b> 로그아웃은 브라우저에서 토큰을 지우는 것일 뿐, 서버는 그 토큰을 계속 유효한
 * 것으로 본다. 누군가 그 토큰을 미리 복사해 뒀다면 만료될 때까지 그대로 쓸 수 있다. 그래서 "이 토큰은
 * 폐기됐다"는 목록을 두고 {@link JwtAuthenticationFilter} 가 매 요청 확인한다.
 *
 * <p><b>왜 토큰 전체가 아니라 {@code jti} 인가.</b> 토큰 문자열은 길고, 목록에 담을 이유가 없다. 토큰마다
 * 붙은 고유번호 하나면 지목할 수 있다.
 *
 * <p><b>왜 남은 시간만 보관하는가.</b> 어차피 만료될 토큰을 그 뒤까지 기억할 필요가 없다. 남은 시간을
 * TTL 로 주면 Redis 가 알아서 지워서 목록이 무한정 커지지 않는다.
 */
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private static final String KEY_PREFIX = "blacklist:";

    /** 값은 쓰지 않는다. 키의 존재 자체가 정보다. */
    private static final String MARKER = "1";

    private final StringRedisTemplate redisTemplate;

    /**
     * 토큰을 폐기 목록에 올린다.
     *
     * @param jti 토큰 고유번호
     * @param ttlMs 토큰이 만료되기까지 남은 시간. 0 이하면 이미 만료된 토큰이라 올릴 이유가 없다.
     */
    public void add(String jti, long ttlMs) {
        if (jti == null || ttlMs <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(key(jti), MARKER, Duration.ofMillis(ttlMs));
    }

    /** 폐기된 토큰인지 확인한다. */
    public boolean contains(String jti) {
        return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }
}
