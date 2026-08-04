package com.financematch.auth.mapper;

import com.financematch.auth.domain.Member;
import com.financematch.auth.domain.MemberAgreement;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {

    boolean existsByEmail(String email);

    int insert(Member member);

    int insertAgreement(MemberAgreement memberAgreement);

    /** 로그인 대상 회원. 탈퇴 회원은 조회되지 않으므로 결과가 null 이면 로그인 실패로 처리한다. */
    Member findByEmail(String email);

    /** 로그인 회원 자신의 기본 정보. 탈퇴 회원은 조회되지 않는다. */
    Member findById(Long memberId);

    int updateLastLoginAt(Long memberId);

    /**
     * 탈퇴 회원까지 포함해 조회한다. 회원탈퇴 전용이다.
     *
     * <p>{@link #findByEmail} 은 탈퇴 회원을 SQL 에서 제외하므로 여기에 쓸 수 없다 — 이미 탈퇴한 회원이
     * {@code MEMBER_NOT_FOUND}(404) 로 보여 {@code MEMBER_ALREADY_WITHDRAWN}(409) 을 구분할 수 없다.
     */
    Member findByIdIncludingWithdrawn(Long memberId);

    /**
     * 회원을 탈퇴 상태로 바꾼다(soft delete). 행을 지우지 않는다.
     *
     * @return 변경된 행 수. 이미 탈퇴한 회원이면 0 이다(동시 요청 방어).
     */
    int withdraw(@Param("memberId") Long memberId, @Param("withdrawnAt") LocalDateTime withdrawnAt);
}
