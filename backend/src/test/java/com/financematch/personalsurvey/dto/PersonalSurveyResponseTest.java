package com.financematch.personalsurvey.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.financematch.personalsurvey.domain.PersonalInvestmentType;
import com.financematch.personalsurvey.domain.PersonalSurveyResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PersonalSurveyResponseTest {

    /** 개인 투자성향 결과를 이름과 투자성향 설명이 담긴 응답으로 변환하는지 검증한다. */
    @ParameterizedTest
    @EnumSource(PersonalInvestmentType.class)
    void convertsPersonalSurveyResultToResponse(PersonalInvestmentType investmentType) {
        PersonalSurveyResult result = mock(PersonalSurveyResult.class);
        when(result.getName()).thenReturn("김하나");
        when(result.getInvestmentType()).thenReturn(investmentType);

        PersonalSurveyResponse response = PersonalSurveyResponse.of(result);

        assertEquals("김하나", response.getName());
        assertEquals(investmentType.getKoreanName(), response.getInvestmentType());
        assertEquals(investmentType.getHeadline(), response.getHeadline());
        assertEquals(investmentType.getDescription(), response.getDescription());
    }
}
