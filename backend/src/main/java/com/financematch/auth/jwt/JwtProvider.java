package com.financematch.auth.jwt;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 발급기.
 *
 * <p>회원 ID 를 {@code subject} 에 담아 서명한 access 토큰을 만든다. 토큰 내용은 암호화가 아니라
 * Base64 인코딩이므로 누구나 열어볼 수 있다. 서명이 막는 것은 열람이 아니라 위조다. 따라서 회원 ID 외의
 * 개인정보(이메일·이름 등)는 담지 않는다.
 *
 * <p>비밀키와 만료 시간은 {@code application-<profile>.properties} 에서 주입받는다. 운영에서는
 * {@code jwt.secret} 을 환경변수로 덮어쓴다.
 */
@Component
public class JwtProvider {

    private final Key key;
    private final long accessTokenValidityMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs) {
        // HS256 은 최소 32바이트 키를 요구한다. 짧으면 여기서 WeakKeyException 이 나 기동이 실패한다.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
    }

    /**
     * 회원 ID 를 담은 access 토큰을 발급한다.
     *
     * @param memberId 토큰의 주체가 되는 회원 ID
     * @return 서명된 JWT 문자열
     */
    public String createAccessToken(Long memberId) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰의 서명·만료를 검증하고 회원 ID 를 꺼낸다.
     *
     * @throws ApiException 만료된 토큰이면 {@code EXPIRED_TOKEN}, 서명이 맞지 않거나 형태가 잘못됐으면
     *     {@code INVALID_TOKEN}
     */
    public Long getMemberId(String token) {
        try {
            String subject =
                    Jwts.parserBuilder()
                            .setSigningKey(key)
                            .build()
                            .parseClaimsJws(token)
                            .getBody()
                            .getSubject();

            return Long.valueOf(subject);

            // ExpiredJwtException 은 JwtException 의 하위 타입이라 반드시 먼저 잡는다.
        } catch (ExpiredJwtException e) {
            throw new ApiException(ErrorCode.EXPIRED_TOKEN);

            // JwtException: 서명 불일치·형식 오류.
            // IllegalArgumentException: 토큰이 null·빈 문자열이거나 subject 가 숫자가 아닌 경우.
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
    }
}
