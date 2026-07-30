package com.financematch.auth.dto;

import com.financematch.auth.domain.Gender;
import java.time.LocalDate;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청.
 *
 * <p>비밀번호 확인({@code passwordConfirm})은 프론트에서만 검증하고 서버로 보내지 않는다.
 */
@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이름을 입력해 주세요.")
    @Size(max = 50, message = "이름은 50자를 넘을 수 없습니다.")
    private String name;

    @NotNull(message = "성별을 선택해 주세요.")
    private Gender gender;

    @NotNull(message = "생년월일을 입력해 주세요.")
    @Past(message = "생년월일이 올바르지 않습니다.")
    private LocalDate birthDate;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 255, message = "이메일은 255자를 넘을 수 없습니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
    private String password;

    // @Valid 가 있어야 중첩 객체 안의 어노테이션까지 검증된다.
    @NotNull(message = "약관 동의 정보가 필요합니다.")
    @Valid
    private SignupAgreementRequest agreements;
}
