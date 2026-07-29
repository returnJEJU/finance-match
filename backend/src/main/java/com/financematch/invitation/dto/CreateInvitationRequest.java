package com.financematch.invitation.dto;

import com.financematch.invitation.domain.CommonSurveyLoanPurpose;
import com.financematch.invitation.domain.GoalType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class CreateInvitationRequest {

    @NotNull(message = "공동 설문 입력값이 올바르지 않습니다.")
    private GoalType goalType1;

    @NotNull(message = "공동 설문 입력값이 올바르지 않습니다.")
    private GoalType goalType2;

    @NotNull(message = "공동 설문 입력값이 올바르지 않습니다.")
    @Positive(message = "공동 설문 입력값이 올바르지 않습니다.")
    @Digits(integer = 15, fraction = 0, message = "공동 설문 입력값이 올바르지 않습니다.")
    private BigDecimal targetAmount;

    @NotNull(message = "공동 설문 입력값이 올바르지 않습니다.")
    @Positive(message = "공동 설문 입력값이 올바르지 않습니다.")
    private Integer targetPeriodMonths;

    @NotNull(message = "공동 설문 입력값이 올바르지 않습니다.")
    private CommonSurveyLoanPurpose loanPurpose;

    @NotNull(message = "공동 설문 입력값이 올바르지 않습니다.")
    private Boolean hasLoanWithinOneMonth;
}
