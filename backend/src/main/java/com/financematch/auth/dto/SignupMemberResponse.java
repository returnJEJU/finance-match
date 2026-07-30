package com.financematch.auth.dto;

import com.financematch.auth.domain.Member;
import lombok.Getter;

/** 회원가입 응답의 {@code member} 부분. 비밀번호는 해시라도 응답에 담지 않는다. */
@Getter
public class SignupMemberResponse {

    private final Long id;
    private final String email;
    private final String name;

    private SignupMemberResponse(Long id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    public static SignupMemberResponse from(Member member) {
        return new SignupMemberResponse(member.getId(), member.getEmail(), member.getName());
    }
}
