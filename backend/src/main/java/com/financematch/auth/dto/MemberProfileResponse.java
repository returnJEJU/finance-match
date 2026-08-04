package com.financematch.auth.dto;

import com.financematch.auth.domain.Member;
import lombok.Getter;

/** 로그인 회원 자신의 기본 프로필. 화면 표시용 이름만 노출한다. */
@Getter
public class MemberProfileResponse {

    private final Long id;
    private final String name;

    private MemberProfileResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static MemberProfileResponse of(Long id, String name) {
        return new MemberProfileResponse(id, name);
    }

    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(member.getId(), member.getName());
    }
}
