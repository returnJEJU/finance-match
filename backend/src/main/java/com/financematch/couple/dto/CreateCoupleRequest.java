package com.financematch.couple.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@NoArgsConstructor
public class CreateCoupleRequest {

    @NotBlank(message = "초대코드 형식이 올바르지 않습니다.")
    @Pattern(regexp = "[A-Z0-9]{8}", message = "초대코드 형식이 올바르지 않습니다.")
    private String inviteCode;
}
