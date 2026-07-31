package com.financematch.personalsurvey.domain;

import com.financematch.personalsurvey.dto.PersonalSurveyRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class PersonalSurvey {

    @Setter
    private Long id;

    private Long memberId;
    private BigDecimal annualIncome;
    private BigDecimal monthlyAvailableAmount;
    private FinancialAssetRatio financialAssetRatio;
    private FinancialKnowledge financialKnowledge;
    private CapitalPreservationAttitude capitalPreservationAttitude;

    public static PersonalSurvey of(Long memberId, PersonalSurveyRequest request) {

        PersonalSurvey survey = new PersonalSurvey();
        survey.memberId = memberId;
        survey.annualIncome = request.getAnnualIncome();
        survey.monthlyAvailableAmount = request.getMonthlyAvailableAmount();
        survey.financialAssetRatio = request.getFinancialAssetRatio();
        survey.financialKnowledge = request.getFinancialKnowledge();
        survey.capitalPreservationAttitude = request.getCapitalPreservationAttitude();

        return survey;
    }

}
