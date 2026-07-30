package com.financematch.auth.mapper;

import com.financematch.auth.domain.Member;
import com.financematch.auth.domain.MemberAgreement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper {

    boolean existsByEmail(String email);

    int insert(Member member);

    int insertAgreement(MemberAgreement memberAgreement);

    /** 로그인 대상 회원. 탈퇴 회원은 조회되지 않으므로 결과가 null 이면 로그인 실패로 처리한다. */
    Member findByEmail(String email);

    int updateLastLoginAt(Long memberId);
}
