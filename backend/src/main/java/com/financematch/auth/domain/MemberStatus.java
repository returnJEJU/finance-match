package com.financematch.auth.domain;

/**
 * 회원 상태.
 *
 * <p>enum 이름이 그대로 {@code member.status} 컬럼에 저장된다. 이름을 바꾸면 DB 값과 어긋나므로 바꾸지
 * 않는다.
 *
 * <p>탈퇴는 행을 지우지 않고 {@link #WITHDRAWN} 으로 바꾼 뒤 {@code deleted_at} 을 기록한다(soft
 * delete). 회원 행에는 커플·설문·리포트·추천이 외래키로 매달려 있어 실제로 지우면 함께 무너지고, "탈퇴했다"는
 * 사실 자체도 감사·복구를 위해 남아야 한다.
 */
public enum MemberStatus {
    ACTIVE,
    /** 휴면. 처리 요구사항이 확정되지 않아 아직 아무도 이 값으로 바꾸지 않는다. */
    DORMANT,
    WITHDRAWN
}
