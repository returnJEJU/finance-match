package com.financematch.couple.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CoupleDisconnectTarget {

    // 삭제할 couple row ID
    private Long coupleId;

    // 커플을 구성하는 두 회원 ID
    private Long inviterId;
    private Long inviteeId;

    // 커플 생성에 사용된 초대코드와 공동설문 ID
    private Long invitationCodeId;
    private Long commonSurveyId;
}
