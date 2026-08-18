package com.financematch.auth.jwt;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * refresh 토큰 보관소 (Redis).
 *
 * <p><b>왜 서버가 보관하는가.</b> JWT 는 서명만 맞으면 유효하므로, 발급한 뒤에는 서버가 취소할 방법이
 * 없다. 그래서 "지금 살아 있는 refresh 토큰"을 회원마다 하나씩 적어두고, 재발급 요청이 오면 적어둔 것과
 * 같은지 대조한다. 로그아웃하면 이 기록을 지워서 그 토큰을 못 쓰게 만든다.
 *
 * <p><b>왜 Redis 인가.</b> 두 가지 때문이다. ①만료 시간을 걸어두면 2주 뒤 알아서 사라진다 — DB 라면
 * 청소하는 배치를 따로 만들어야 한다. ②앱을 재시작해도 남아 있다 — 메모리에 들고 있으면 배포할 때마다
 * 모두 로그아웃된다.
 *
 * <p><b>회원당 한 개만 둔다.</b> 새로 로그인하거나 재발급받으면 이전 것을 덮어써서 무효가 된다. 여러
 * 기기에서 동시에 로그인 상태를 유지하려면 키에 기기 구분을 더해야 하지만, 지금은 필요하지 않다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    /** 회원의 refresh 토큰을 저장한다. 기존 값이 있으면 덮어쓴다(= 이전 토큰 무효화). */
    public void save(Long memberId, String refreshToken, long ttlMs) {
        redisTemplate.opsForValue().set(key(memberId), refreshToken, Duration.ofMillis(ttlMs));
    }

    /**
     * 서버가 보관 중인 토큰과 같은지 확인한다.
     *
     * <p>토큰 문자열 자체가 비밀값이므로 {@code equals} 로 비교한다. 저장된 값이 없으면(로그아웃했거나
     * 만료됐거나) 항상 false 다.
     */
    public boolean matches(Long memberId, String refreshToken) {
        String saved = redisTemplate.opsForValue().get(key(memberId));
        return saved != null && saved.equals(refreshToken);
    }

    /** 저장된 토큰을 지운다. 로그아웃·탈퇴 시 호출한다. */
    public void delete(Long memberId) {
        redisTemplate.delete(key(memberId));
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}
