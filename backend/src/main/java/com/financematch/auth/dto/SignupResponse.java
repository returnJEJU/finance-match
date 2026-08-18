package com.financematch.auth.dto;

import com.financematch.auth.domain.Member;
import lombok.Getter;

/**
 * 회원가입 응답.
 *
 * <p>{@code accessToken} 은 다음 단계인 자산연동({@code POST /v1/members/me/assets})을 인증하기 위한
 * 토큰이다. HTTP 는 이전 요청을 기억하지 못하므로, 가입과 자산연동을 잇는 수단으로 발급한다.
 *
 * <pre>
 * { "member": { "id": 1, "email": "hong@kb.com", "name": "홍길동" },
 *   "accessToken": "eyJ...",
 *   "refreshToken": "eyJ..." }
 * </pre>
 */
@Getter
public class SignupResponse {

    private final SignupMemberResponse member;
    private final String accessToken;

    /** 가입 직후에도 토큰이 만료될 수 있으므로 로그인과 같은 한 쌍을 준다. */
    private final String refreshToken;

    private SignupResponse(
            SignupMemberResponse member, String accessToken, String refreshToken) {
        this.member = member;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static SignupResponse of(Member member, String accessToken, String refreshToken) {
        return new SignupResponse(
                SignupMemberResponse.from(member), accessToken, refreshToken);
    }
}
