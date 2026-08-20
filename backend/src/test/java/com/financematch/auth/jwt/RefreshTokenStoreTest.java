package com.financematch.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
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
 * refresh 토큰 보관소 검증.
 *
 * <p>JWT 는 서명만 맞으면 유효해서 발급한 뒤에는 서버가 취소할 수 없다. 그래서 "지금 살아 있는 refresh
 * 토큰" 을 회원마다 적어두고 재발급 요청 때 대조한다. <b>대조가 헐거우면 로그아웃한 토큰이나 이미 쓴
 * 토큰으로도 재발급이 된다.</b>
 *
 * <p>회원당 한 개만 두는 것이 규칙이다 — 새로 로그인하거나 재발급받으면 이전 것이 덮여 무효가 된다.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

    private static final Long MEMBER_ID = 42L;
    private static final String EXPECTED_KEY = "refresh:" + MEMBER_ID;
    private static final String TOKEN = "saved.refresh.token";
    private static final long TWO_WEEKS_MS = 1_209_600_000L;

    @Mock private StringRedisTemplate redisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private RefreshTokenStore refreshTokenStore;

    // ===== 저장 =====

    @Test
    void 회원별_키에_유효기간을_걸어_저장한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        refreshTokenStore.save(MEMBER_ID, TOKEN, TWO_WEEKS_MS);

        // TTL 이 없으면 2주 뒤 청소하는 배치를 따로 만들어야 한다.
        verify(valueOperations).set(EXPECTED_KEY, TOKEN, Duration.ofMillis(TWO_WEEKS_MS));
    }

    @Test
    void 같은_회원이_다시_저장하면_이전_토큰을_덮어쓴다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        refreshTokenStore.save(MEMBER_ID, "old.token", TWO_WEEKS_MS);
        refreshTokenStore.save(MEMBER_ID, "new.token", TWO_WEEKS_MS);

        // 회원당 한 개가 규칙이다. 키가 같아야 이전 토큰이 무효가 된다.
        verify(valueOperations).set(EXPECTED_KEY, "old.token", Duration.ofMillis(TWO_WEEKS_MS));
        verify(valueOperations).set(EXPECTED_KEY, "new.token", Duration.ofMillis(TWO_WEEKS_MS));
    }

    // ===== 대조 =====

    @Test
    void 보관_중인_토큰과_같으면_통과시킨다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(TOKEN);

        assertTrue(refreshTokenStore.matches(MEMBER_ID, TOKEN));
    }

    @Test
    void 보관_중인_것과_다른_토큰은_거절한다() {
        // 이미 한 번 재발급에 써서 새 토큰으로 덮인 경우가 여기 해당한다.
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(EXPECTED_KEY)).thenReturn("another.token");

        assertFalse(refreshTokenStore.matches(MEMBER_ID, TOKEN));
    }

    @Test
    void 보관된_토큰이_없으면_거절한다() {
        // 로그아웃했거나 만료돼 사라진 경우다. 서명이 멀쩡해도 재발급해 주면 안 된다.
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(null);

        assertFalse(refreshTokenStore.matches(MEMBER_ID, TOKEN));
    }

    @Test
    void 남의_토큰으로는_통과하지_못한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:99")).thenReturn("someone.else.token");

        assertFalse(refreshTokenStore.matches(99L, TOKEN));
    }

    // ===== 삭제 =====

    @Test
    void 로그아웃하면_보관된_토큰을_지운다() {
        refreshTokenStore.delete(MEMBER_ID);

        // 지우지 않으면 로그아웃해도 refresh 토큰으로 다시 access 토큰을 받을 수 있다.
        verify(redisTemplate).delete(EXPECTED_KEY);
    }
}
