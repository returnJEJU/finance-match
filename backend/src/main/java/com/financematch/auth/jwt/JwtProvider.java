package com.financematch.auth.jwt;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 발급기.
 *
 * <p>회원 ID 를 {@code subject} 에 담아 서명한 토큰을 만든다. 토큰 내용은 암호화가 아니라 Base64
 * 인코딩이므로 누구나 열어볼 수 있다. 서명이 막는 것은 열람이 아니라 위조다. 따라서 회원 ID 외의
 * 개인정보(이메일·이름 등)는 담지 않는다.
 *
 * <p><b>토큰은 두 종류다.</b>
 *
 * <ul>
 *   <li><b>access</b> — 매 요청에 실려 회원을 증명한다. 짧다(운영 1시간).
 *   <li><b>refresh</b> — access 가 만료됐을 때 새로 받아오는 용도. 길다(2주). 서버가 Redis 에
 *       보관하고 있는 것과 일치해야만 인정한다({@link RefreshTokenStore}).
 * </ul>
 *
 * <p>둘을 {@code typ} 클레임으로 구분한다. 이 구분이 없으면 유효기간이 긴 refresh 토큰을 access 자리에
 * 그대로 써서 2주짜리 통행증처럼 쓸 수 있다.
 *
 * <p>모든 토큰에 {@code jti}(토큰 고유번호)를 넣는다. 로그아웃한 토큰을 만료 전에 막으려면 "어떤
 * 토큰인가"를 가리킬 이름이 필요하기 때문이다({@link TokenBlacklist}).
 *
 * <p>비밀키와 만료 시간은 {@code application-<profile>.properties} 에서 주입받는다. 운영에서는
 * {@code jwt.secret} 을 환경변수로 덮어쓴다.
 */
@Component
public class JwtProvider {

    /** 토큰 종류를 담는 클레임 이름. */
    private static final String CLAIM_TYPE = "typ";

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final Key key;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs) {
        // HS256 은 최소 32바이트 키를 요구한다. 짧으면 여기서 WeakKeyException 이 나 기동이 실패한다.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    /** 회원 ID 를 담은 access 토큰을 발급한다. */
    public String createAccessToken(Long memberId) {
        return create(memberId, TYPE_ACCESS, accessTokenValidityMs);
    }

    /** 회원 ID 를 담은 refresh 토큰을 발급한다. 발급한 쪽이 {@link RefreshTokenStore} 에 저장해야 한다. */
    public String createRefreshToken(Long memberId) {
        return create(memberId, TYPE_REFRESH, refreshTokenValidityMs);
    }

    private String create(Long memberId, String type, long validityMs) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + validityMs);

        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                // 토큰마다 다른 고유번호. 같은 회원이 같은 순간에 두 번 발급받아도 서로 다른 토큰이 된다.
                .setId(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, type)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * access 토큰을 검증하고 회원 ID 를 꺼낸다.
     *
     * @throws ApiException 만료면 {@code EXPIRED_TOKEN}, 서명 불일치·형식 오류·refresh 토큰이면 {@code
     *     INVALID_TOKEN}
     */
    public Long getMemberId(String token) {
        Claims claims = parse(token);

        // typ 이 없는 토큰은 이 기능을 넣기 전에 발급된 access 토큰이다. 배포 직후 모든 사용자의
        // 로그인이 한꺼번에 풀리지 않도록 access 로 취급한다. (2주 뒤에는 전부 사라진다)
        String type = claims.get(CLAIM_TYPE, String.class);
        if (type != null && !TYPE_ACCESS.equals(type)) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
        return toMemberId(claims);
    }

    /**
     * refresh 토큰을 검증하고 회원 ID 를 꺼낸다.
     *
     * <p>여기를 통과했다고 재발급해 주면 안 된다. 서버가 보관 중인 값과 같은지 반드시 함께 확인한다 —
     * 그래야 로그아웃한 토큰이나 이미 한 번 쓴 토큰을 걸러낼 수 있다.
     *
     * @throws ApiException 만료·위조이거나 access 토큰이면 {@code INVALID_REFRESH_TOKEN}
     */
    public Long getMemberIdFromRefreshToken(String token) {
        Claims claims;
        try {
            claims = parse(token);
        } catch (ApiException e) {
            // 만료든 위조든 결과는 같다 — 다시 로그인해야 한다. 프론트가 이 코드를 보고
            // 재발급 재시도 대신 로그인 화면으로 보낸다.
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return toMemberId(claims);
    }

    /** 토큰의 고유번호({@code jti}). 블랙리스트의 키로 쓴다. */
    public String getJti(String token) {
        return parse(token).getId();
    }

    /**
     * 토큰이 만료되기까지 남은 시간(ms).
     *
     * <p>블랙리스트 TTL 로 쓴다. 어차피 만료될 토큰을 그 뒤까지 기억할 이유가 없다 — 남은 시간만 막으면
     * 되고, 그러면 Redis 가 알아서 지워 준다.
     */
    public long getRemainingMs(String token) {
        long remaining = parse(token).getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    /** refresh 토큰 유효기간(ms). 저장소 TTL 을 토큰 만료와 맞추는 데 쓴다. */
    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }

    private Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // ExpiredJwtException 은 JwtException 의 하위 타입이라 반드시 먼저 잡는다.
        } catch (ExpiredJwtException e) {
            throw new ApiException(ErrorCode.EXPIRED_TOKEN);

            // JwtException: 서명 불일치·형식 오류.
            // IllegalArgumentException: 토큰이 null·빈 문자열인 경우.
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
    }

    private Long toMemberId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
    }
}
