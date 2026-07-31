package com.financematch.auth.dto;

import com.financematch.auth.domain.MemberStatus;
import java.time.LocalDateTime;
import lombok.Getter;

/** 회원탈퇴 응답. 탈퇴 처리된 회원 ID·변경된 상태·처리 일시를 돌려준다. */
@Getter
public class WithdrawResponse {

    private final Long memberId;
    private final MemberStatus status;
    private final LocalDateTime withdrawnAt;

    private WithdrawResponse(Long memberId, MemberStatus status, LocalDateTime withdrawnAt) {
        this.memberId = memberId;
        this.status = status;
        this.withdrawnAt = withdrawnAt;
    }

    public static WithdrawResponse of(Long memberId, LocalDateTime withdrawnAt) {
        return new WithdrawResponse(memberId, MemberStatus.WITHDRAWN, withdrawnAt);
    }
}
