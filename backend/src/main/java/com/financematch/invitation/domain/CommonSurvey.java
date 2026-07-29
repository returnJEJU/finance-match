package com.financematch.invitation.domain;

import com.financematch.invitation.dto.CreateInvitationRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class CommonSurvey {

    @Setter // MyBatis가 자동 생성된 PK를 넣기 위해 필요
    private Long id;

    private Long memberId;
    private GoalType firstGoalType;
    private GoalType secondGoalType;
    private BigDecimal targetAmount;
    private Integer targetPeriodMonths;
    private CommonSurveyLoanPurpose loanPurpose;
    private Boolean hasLoanWithinOneMonth;

    // API 요청 DTO를 DB 저장용 domain 객체로 변환하는 메서드
    public static CommonSurvey of(Long memberId, CreateInvitationRequest request) {

        CommonSurvey survey = new CommonSurvey();
        survey.memberId = memberId;
        survey.firstGoalType = request.getGoalType1();
        survey.secondGoalType = request.getGoalType2();
        survey.targetAmount = request.getTargetAmount();
        survey.targetPeriodMonths = request.getTargetPeriodMonths();
        survey.loanPurpose = request.getLoanPurpose();
        survey.hasLoanWithinOneMonth = request.getHasLoanWithinOneMonth();

        return survey;
    }

}
