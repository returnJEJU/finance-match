package com.financematch.personalsurvey.dto;

import com.financematch.personalsurvey.domain.PersonalInvestmentType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PersonalSurveyResponse {

    private final String investmentType;
    private final String description;
    private final String characterImageName;

    public static PersonalSurveyResponse of(PersonalInvestmentType investmentType) {
        return new PersonalSurveyResponse(
                investmentType.getKoreanName(),
                investmentType.getDescription(),
                investmentType.getCharacterImageName()
        );
    }
}
