package com.financematch.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 로그아웃한 access 토큰 폐기 목록 검증.
 *
 * <p>로그아웃은 브라우저에서 토큰을 지우는 것일 뿐이라, 누군가 그 토큰을 미리 복사해 뒀다면 만료될
 * 때까지 그대로 쓸 수 있다. 이 목록이 그 구멍을 막는다 — <b>여기가 새면 로그아웃이 로그아웃이 아니게
 * 된다.</b>
 *
 * <p>Redis 를 실제로 띄우지 않고 목으로 확인한다. 검증 대상은 Redis 의 동작이 아니라 <b>어떤 키에
 * 무엇을 얼마 동안 넣는가</b> 라는 이 클래스의 규칙이다.
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistTest {

    private static final String JTI = "6f0a1c2e-3b4d";
    private static final String EXPECTED_KEY = "blacklist:" + JTI;
    private static final long ONE_HOUR_MS = 3_600_000L;

    @Mock private StringRedisTemplate redisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private TokenBlacklist tokenBlacklist;

    // ===== 폐기 등록 =====

    @Test
    void 폐기한_토큰은_접두사를_붙인_키에_남은_시간만큼만_보관한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklist.add(JTI, ONE_HOUR_MS);

        // 접두사가 없으면 다른 용도의 키와 섞이고, TTL 이 없으면 목록이 무한정 커진다.
        verify(valueOperations).set(EXPECTED_KEY, "1", Duration.ofMillis(ONE_HOUR_MS));
    }

    @Test
    void 이미_만료된_토큰은_목록에_올리지_않는다() {
        // 남은 시간이 0 이하면 그 토큰은 이미 못 쓴다. 올려봐야 자리만 차지한다.
        tokenBlacklist.add(JTI, 0L);
        tokenBlacklist.add(JTI, -1_000L);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void jti_가_없으면_목록에_올리지_않는다() {
        // 막지 않으면 "blacklist:null" 이라는 엉뚱한 항목이 쌓인다.
        tokenBlacklist.add(null, ONE_HOUR_MS);

        verifyNoInteractions(redisTemplate);
    }

    // ===== 폐기 여부 확인 =====

    @Test
    void 등록할_때_쓴_키로_그대로_조회한다() {
        // 등록 키와 조회 키가 어긋나면 폐기했는데도 계속 통과하는 상태가 된다.
        when(redisTemplate.hasKey(EXPECTED_KEY)).thenReturn(true);

        assertTrue(tokenBlacklist.contains(JTI));
    }

    @Test
    void 목록에_없는_토큰은_그대로_쓸_수_있다() {
        when(redisTemplate.hasKey(EXPECTED_KEY)).thenReturn(false);

        assertFalse(tokenBlacklist.contains(JTI));
    }

    @Test
    void Redis_가_null_을_돌려줘도_폐기로_오인하지_않는다() {
        // hasKey 의 반환형은 Boolean 이라 null 이 올 수 있다. 그대로 풀면 NullPointerException 이다.
        when(redisTemplate.hasKey(EXPECTED_KEY)).thenReturn(null);

        assertFalse(tokenBlacklist.contains(JTI));
    }

    @Test
    void jti_가_없으면_Redis_를_조회하지도_않는다() {
        assertFalse(tokenBlacklist.contains(null));

        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    void 토큰마다_키가_구분된다() {
        when(redisTemplate.hasKey("blacklist:aaa")).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:bbb")).thenReturn(false);

        assertTrue(tokenBlacklist.contains("aaa"));
        assertFalse(tokenBlacklist.contains("bbb"));
    }
}
