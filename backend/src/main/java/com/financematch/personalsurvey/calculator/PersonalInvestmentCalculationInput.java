package com.financematch.personalsurvey.calculator;

import com.financematch.invitation.domain.GoalType;
import com.financematch.personalsurvey.domain.CapitalPreservationAttitude;
import com.financematch.personalsurvey.domain.FinancialAssetRatio;
import com.financematch.personalsurvey.domain.FinancialKnowledge;
import com.financematch.personalsurvey.domain.InvestmentExperience;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Builder
public class PersonalInvestmentCalculationInput {

    private LocalDate birthDate;
    private long annualIncome;
    private FinancialAssetRatio financialAssetRatio;
    private Set<InvestmentExperience> investmentExperiences;
    private FinancialKnowledge financialKnowledge;
    private CapitalPreservationAttitude capitalPreservationAttitude;
    private GoalType firstGoalType;
    private Integer targetPeriodMonths;
}
