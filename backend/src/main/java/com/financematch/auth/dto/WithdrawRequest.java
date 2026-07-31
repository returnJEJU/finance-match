package com.financematch.auth.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원탈퇴 요청.
 *
 * <p>토큰만으로 탈퇴시키지 않는다. 되돌리기 어려운 동작이라 비밀번호(본인 확인)와 확인 문구(실수 방지)를 함께
 * 받는다. 값의 일치 여부는 {@code MemberService} 에서 검증한다 — 어노테이션 검증으로는 에러 코드를 나눠
 * 지정할 수 없다.
 */
@Getter
@NoArgsConstructor
public class WithdrawRequest {

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;

    @NotBlank(message = "확인 문구를 입력해 주세요.")
    private String confirmationText;
}
