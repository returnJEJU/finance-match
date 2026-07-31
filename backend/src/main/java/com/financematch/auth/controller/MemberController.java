package com.financematch.auth.controller;

import com.financematch.auth.annotation.LoginMember;
import com.financematch.auth.dto.WithdrawRequest;
import com.financematch.auth.dto.WithdrawResponse;
import com.financematch.auth.service.MemberService;
import com.financematch.common.ApiResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 회원 자신의 계정 API.
 *
 * <p>회원 식별자를 경로에 두지 않고 {@code /v1/members/me} 아래에 둔다. 남의 계정을 지목할 수 없게 해
 * IDOR 을 차단한다 — 대상은 항상 토큰이 가리키는 회원이다.
 *
 * <p>{@code Member} 와 {@code MemberMapper} 가 {@code auth} 패키지에 있어 컨트롤러도 같은 곳에 둔다.
 * 별도 패키지를 만들면 같은 도메인이 둘로 쪼개진다.
 */
@RestController
@RequestMapping("/v1/members/me")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원탈퇴. 행을 지우지 않고 상태만 {@code WITHDRAWN} 으로 바꾼다.
     *
     * <p>DELETE 인데 본문을 받는다 — 본인 확인용 비밀번호와 확인 문구가 필요하기 때문이다. 되돌리기 어려운
     * 동작이라 토큰만으로 처리하지 않는다.
     */
    @DeleteMapping
    public ApiResponse<WithdrawResponse> withdraw(
            @LoginMember Long memberId, @Valid @RequestBody WithdrawRequest request) {

        return ApiResponse.ok(memberService.withdraw(memberId, request));
    }
}
