package com.financematch.auth.dto;

import com.financematch.auth.domain.Member;
import lombok.Getter;

/** 로그인 응답의 {@code member} 부분. 명세상 이메일은 담지 않는다(로그인 화면에서 이미 입력한 값). */
@Getter
public class LoginMemberResponse {

    private final Long id;
    private final String name;

    private LoginMemberResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static LoginMemberResponse from(Member member) {
        return new LoginMemberResponse(member.getId(), member.getName());
    }
}
