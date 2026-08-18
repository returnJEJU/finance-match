package com.financematch.auth.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * access 토큰 재발급 요청.
 *
 * <pre>
 * { "refreshToken": "eyJ..." }
 * </pre>
 *
 * <p>refresh 토큰을 헤더가 아니라 본문으로 받는다. {@code Authorization} 헤더는 access 토큰 자리이고,
 * 이 요청은 그 access 토큰이 만료된 상황에서 호출되기 때문이다.
 */
@Getter
@NoArgsConstructor
public class TokenReissueRequest {

    @NotBlank(message = "refreshToken 이 필요합니다.")
    private String refreshToken;
}
