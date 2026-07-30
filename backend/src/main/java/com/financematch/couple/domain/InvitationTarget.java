package com.financematch.couple.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InvitationTarget {

    private Long invitationCodeId; // couple.invitation_code_id 저장 및 USED 변경
    private Long commonSurveyId; // 초대코드가 연결된 공동설문 식별
    private Long inviterId; // 초대코드 생성자 B 식별 및 자기 초대, 커플 여부 검사
    private String status; // ACTIVE 여부 검사
}
