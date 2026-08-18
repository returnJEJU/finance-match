package com.financematch.auth.dto;

import lombok.Getter;

/**
 * 토큰 재발급 응답.
 *
 * <pre>
 * { "accessToken": "eyJ...", "refreshToken": "eyJ..." }
 * </pre>
 *
 * <p><b>refresh 토큰도 함께 새로 준다(회전).</b> 한 번 쓴 refresh 토큰은 그 즉시 무효가 되므로, 토큰이
 * 새어 나가도 원래 사용자가 다음 재발급을 하는 순간 공격자의 것은 못 쓰게 된다. 프론트는 응답으로 받은
 * 두 값을 모두 갈아 끼워야 한다 — access 만 바꾸면 다음 재발급에서 로그인이 풀린다.
 */
@Getter
public class TokenResponse {

    private final String accessToken;
    private final String refreshToken;

    private TokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static TokenResponse of(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken);
    }
}
