package com.financematch.invitation.dto;

import com.financematch.invitation.domain.CommonSurveyLoanPurpose;
import com.financematch.invitation.domain.GoalType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class CommonSurveyResponse {

    private GoalType goalType1;
    private GoalType goalType2;
    private BigDecimal targetAmount;
    private Integer targetPeriodMonths;
    private CommonSurveyLoanPurpose loanPurpose;
    private Boolean hasLoanWithinOneMonth;
}
