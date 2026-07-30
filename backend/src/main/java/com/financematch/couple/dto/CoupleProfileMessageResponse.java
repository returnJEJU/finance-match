package com.financematch.couple.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CoupleProfileMessageResponse {

    // 파트너와 함께 보는 커플 한줄 소개
    private final String profileMessage;
}
