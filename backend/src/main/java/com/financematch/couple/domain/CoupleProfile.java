package com.financematch.couple.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CoupleProfile {

    // couple.id: 로그인 회원이 속한 커플 ID
    private Long coupleId;

    // 로그인 회원 이름
    private String myName;

    // 연결된 파트너 이름
    private String partnerName;

    // couple.profile_message: 파트너와 공유하는 커플 한줄 소개
    private String profileMessage;
}
