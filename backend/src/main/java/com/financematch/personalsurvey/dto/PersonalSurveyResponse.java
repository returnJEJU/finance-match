package com.financematch.personalsurvey.dto;

import com.financematch.personalsurvey.domain.PersonalInvestmentType;
import com.financematch.personalsurvey.domain.PersonalSurveyResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PersonalSurveyResponse {

    private final String name;
    private final String investmentType;
    private final String headline;
    private final String description;

    public static PersonalSurveyResponse of(PersonalSurveyResult result) {
        PersonalInvestmentType investmentType = result.getInvestmentType();

        return new PersonalSurveyResponse(
                result.getName(),
                investmentType.getKoreanName(),
                investmentType.getHeadline(),
                investmentType.getDescription()
        );
    }
}
