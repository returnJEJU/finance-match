package com.financematch.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.financematch.auth.domain.Member;
import com.financematch.onboarding.dto.OnboardingStatusResponse;
import lombok.Getter;

/**
 * 로그인 응답.
 *
 * <pre>
 * { "accessToken": "eyJ...",
 *   "refreshToken": "eyJ...",
 *   "member": { "id": 1, "name": "홍길동" },
 *   "isFirstLogin": true,
 *   "progress": { "coupleConnected": false, "surveyCompleted": false } }
 * </pre>
 *
 * <p>{@code isFirstLogin} 은 갱신 전 {@code last_login_at} 이 NULL 이었는지로 판단한다. 갱신하면
 * 사라지는 값이라 로그인 시점에만 알 수 있다.
 *
 * <p>{@code progress} 는 {@code GET /v1/members/me/onboarding-status} 와 같은 타입을 그대로 쓴다.
 * 같은 판단을 두 곳에서 하면 정의가 바뀔 때 한쪽만 고쳐 갈라질 수 있어 온보딩 쪽을 재사용한다. 프론트도
 * 같은 스키마로 파싱할 수 있다.
 */
@Getter
public class LoginResponse {

    private final String accessToken;

    /**
     * access 토큰이 만료됐을 때 새로 받아오는 데 쓴다. 프론트가 보관해 두었다가
     * {@code POST /v1/auth/refresh} 에 실어 보낸다.
     */
    private final String refreshToken;

    private final LoginMemberResponse member;

    /**
     * 필드명을 {@code firstLogin} 으로 두고 JSON 이름만 {@code isFirstLogin} 으로 바꾼다.
     *
     * <p>Lombok 이 만드는 게터는 {@code isFirstLogin()} 인데, Jackson 은 boolean 게터의 {@code is} 를
     * 떼어내 속성명을 {@code firstLogin} 으로 본다. 필드명을 {@code isFirstLogin} 으로 두면 게터가 보는
     * 이름과 달라져 속성이 둘로 갈라지고 {@code isFirstLogin}·{@code firstLogin} 이 모두 응답에 나간다.
     */
    @JsonProperty("isFirstLogin")
    private final boolean firstLogin;

    private final OnboardingStatusResponse progress;

    private LoginResponse(
            String accessToken,
            String refreshToken,
            LoginMemberResponse member,
            boolean isFirstLogin,
            OnboardingStatusResponse progress) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.member = member;
        this.firstLogin = isFirstLogin;
        this.progress = progress;
    }

    public static LoginResponse of(
            Member member,
            String accessToken,
            String refreshToken,
            boolean isFirstLogin,
            OnboardingStatusResponse progress) {

        return new LoginResponse(
                accessToken,
                refreshToken,
                LoginMemberResponse.from(member),
                isFirstLogin,
                progress);
    }
}
