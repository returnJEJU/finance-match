package com.financematch.auth.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 요청.
 *
 * <p>이메일 형식은 검사하지 않는다. 가입 때 이미 검증했고, 로그인 실패는 형식이든 값이든 모두 {@code
 * INVALID_CREDENTIALS} 로 답해야 계정 존재 여부가 드러나지 않는다. 빈 값만 입력 누락으로 걸러낸다.
 */
@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "이메일을 입력해 주세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;
}
