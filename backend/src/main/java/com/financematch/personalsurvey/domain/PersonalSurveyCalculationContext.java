package com.financematch.personalsurvey.domain;

import com.financematch.invitation.domain.GoalType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class PersonalSurveyCalculationContext {

    private LocalDate birthDate;
    private GoalType firstGoalType;
    private Integer targetPeriodMonths;
}
