package com.financematch.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    // HS256 은 32바이트 이상을 요구한다. 아래 문자열은 56바이트.
    private static final String SECRET = "unit-test-secret-key-do-not-use-in-production-1234567890";
    private static final long VALIDITY_MS = 3_600_000L;

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, VALIDITY_MS);

    @Test
    void 발급된_토큰은_점으로_구분된_세_조각이다() {
        String token = jwtProvider.createAccessToken(7L);

        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void 토큰의_subject_에_회원_ID_가_담긴다() {
        String token = jwtProvider.createAccessToken(7L);

        // subject 는 JWT 규격상 문자열이라 Long 이 아니라 "7" 로 들어간다.
        assertEquals("7", parse(token).getSubject());
    }

    @Test
    void 만료_시각은_발급_시각에_설정한_유효기간을_더한_값이다() {
        Claims claims = parse(jwtProvider.createAccessToken(7L));

        long validityMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertEquals(VALIDITY_MS, validityMs);
    }

    @Test
    void 발급한_토큰에서_회원_ID_를_꺼낸다() {
        String token = jwtProvider.createAccessToken(7L);

        assertEquals(7L, jwtProvider.getMemberId(token));
    }

    @Test
    void 다른_비밀키로_서명한_토큰은_INVALID_TOKEN_이다() {
        // 서명이 위조를 막는다는 전제. 이 전제가 깨지면 인증 전체가 무의미해진다.
        JwtProvider other =
                new JwtProvider("another-secret-key-that-is-long-enough-for-hs256-000", VALIDITY_MS);

        String tokenFromOtherKey = other.createAccessToken(7L);

        assertEquals(
                ErrorCode.INVALID_TOKEN,
                assertThrows(ApiException.class, () -> jwtProvider.getMemberId(tokenFromOtherKey))
                        .getErrorCode());
    }

    @Test
    void 만료된_토큰은_EXPIRED_TOKEN_이다() {
        // 유효기간을 음수로 두면 발급 즉시 만료된 토큰이 나온다.
        String expiredToken = new JwtProvider(SECRET, -1_000L).createAccessToken(7L);

        assertEquals(
                ErrorCode.EXPIRED_TOKEN,
                assertThrows(ApiException.class, () -> jwtProvider.getMemberId(expiredToken))
                        .getErrorCode());
    }

    @Test
    void 토큰_형태가_아닌_문자열은_INVALID_TOKEN_이다() {
        assertEquals(
                ErrorCode.INVALID_TOKEN,
                assertThrows(ApiException.class, () -> jwtProvider.getMemberId("not-a-token"))
                        .getErrorCode());
    }

    @Test
    void 토큰이_null_이거나_비어_있으면_INVALID_TOKEN_이다() {
        assertEquals(
                ErrorCode.INVALID_TOKEN,
                assertThrows(ApiException.class, () -> jwtProvider.getMemberId(null))
                        .getErrorCode());
        assertEquals(
                ErrorCode.INVALID_TOKEN,
                assertThrows(ApiException.class, () -> jwtProvider.getMemberId(""))
                        .getErrorCode());
    }

    @Test
    void 비밀키가_32바이트보다_짧으면_객체_생성_단계에서_실패한다() {
        // 약한 키를 조용히 받아들이지 않고 기동 시점에 즉시 실패해야 한다.
        assertThrows(WeakKeyException.class, () -> new JwtProvider("too-short", VALIDITY_MS));
    }

    /** 발급된 토큰을 같은 비밀키로 검증·해석한다. 검증 메서드는 ④ 에서 JwtProvider 에 추가한다. */
    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
