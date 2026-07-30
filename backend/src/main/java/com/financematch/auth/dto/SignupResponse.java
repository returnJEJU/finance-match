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
 *   "accessToken": "eyJ..." }
 * </pre>
 */
@Getter
public class SignupResponse {

    private final SignupMemberResponse member;
    private final String accessToken;

    private SignupResponse(SignupMemberResponse member, String accessToken) {
        this.member = member;
        this.accessToken = accessToken;
    }

    public static SignupResponse of(Member member, String accessToken) {
        return new SignupResponse(SignupMemberResponse.from(member), accessToken);
    }
}
