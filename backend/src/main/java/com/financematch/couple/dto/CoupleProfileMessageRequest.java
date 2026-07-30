package com.financematch.couple.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CoupleProfileMessageRequest {

    // 마이페이지에서 수정하는 커플 공유 한줄 소개
    @NotBlank(message = "커플 한줄 소개를 입력해 주세요.")
    @Size(max = 50, message = "커플 한줄 소개는 50자 이하여야 합니다.")
    private String profileMessage;
}
