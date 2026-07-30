package com.financematch.couple.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CoupleProfile {

    // couple.id: 로그인 회원이 속한 커플 ID
    private Long coupleId;

    // couple.profile_message: 파트너와 공유하는 커플 한줄 소개
    private String profileMessage;
}
