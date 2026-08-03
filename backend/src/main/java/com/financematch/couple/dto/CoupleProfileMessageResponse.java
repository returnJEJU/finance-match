package com.financematch.couple.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CoupleProfileMessageResponse {

    // 로그인 회원 이름
    private final String myName;

    // 연결된 파트너 이름
    private final String partnerName;

    // 파트너와 함께 보는 커플 한줄 소개
    private final String profileMessage;
}
