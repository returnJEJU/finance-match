package com.financematch.auth.service;

import com.financematch.auth.domain.Member;
import com.financematch.auth.dto.WithdrawRequest;
import com.financematch.auth.dto.WithdrawResponse;
import com.financematch.auth.mapper.MemberMapper;
import com.financematch.common.ErrorCode;
import com.financematch.couple.service.CoupleService;
import com.financematch.exception.ApiException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 계정 자체를 다루는 서비스. 인증(가입·로그인·로그아웃)은 {@code AuthService} 가 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    /** 탈퇴 확인 문구. 프론트는 사용자가 이 문구를 그대로 입력하게 하고 값을 보낸다. */
    private static final String CONFIRMATION_TEXT = "회원 탈퇴";

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final CoupleService coupleService;

    /**
     * 회원을 탈퇴 상태로 바꾼다(soft delete).
     *
     * <p>탈퇴 전 커플 연결이 있으면 함께 해제한다. 탈퇴한 회원이 커플 row 에 남으면 상대방의 대시보드와
     * 리포트에 과거 커플 데이터가 계속 보이고, 새 커플 연결도 막힐 수 있기 때문이다.
     *
     * <p>검사 순서는 의도적이다. 문자열 비교로 걸러낼 수 있는 확인 문구를 비밀번호보다 먼저 본다. BCrypt
     * 검증은 무차별 대입을 늦추려고 일부러 느리게 만든 연산이라, 어차피 실패할 요청에 굳이 치르지 않는다.
     */
    @Transactional
    public WithdrawResponse withdraw(Long memberId, WithdrawRequest request) {

        Member member = memberMapper.findByIdIncludingWithdrawn(memberId);

        if (member == null) {
            throw new ApiException(ErrorCode.MEMBER_NOT_FOUND);
        }

        if (member.isWithdrawn()) {
            throw new ApiException(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
        }

        if (!CONFIRMATION_TEXT.equals(request.getConfirmationText())) {
            throw new ApiException(ErrorCode.INVALID_CONFIRMATION);
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_PASSWORD);
        }

        coupleService.disconnectCoupleIfConnected(memberId);

        // member.deleted_at 은 소수점 이하가 없는 datetime 이다. 자르지 않으면 MySQL 이 반올림해
        // 응답으로 알려준 시각과 저장된 값이 최대 1초까지 어긋난다.
        LocalDateTime withdrawnAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // 조회 시점과 갱신 시점 사이에 다른 요청이 먼저 탈퇴시켰다면 0행이 된다.
        if (memberMapper.withdraw(memberId, withdrawnAt) != 1) {
            throw new ApiException(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
        }

        log.info("회원탈퇴 — memberId={}", memberId);

        return WithdrawResponse.of(memberId, withdrawnAt);
    }
}
