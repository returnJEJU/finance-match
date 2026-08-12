package com.financematch.personalsurvey.controller;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.common.ApiResponse;
import com.financematch.personalsurvey.dto.PersonalSurveyRequest;
import com.financematch.personalsurvey.dto.PersonalSurveyResponse;
import com.financematch.personalsurvey.service.PersonalSurveyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonalSurveyControllerTest {

    @Mock
    private PersonalSurveyService personalSurveyService;

    @InjectMocks
    private PersonalSurveyController personalSurveyController;

    /** 로그인 회원의 개인 설문을 서비스에 전달하고 성공 응답의 데이터가 null인지 검증한다. */
    @Test
    void savesPersonalSurveyAndReturnsNullData() {
        PersonalSurveyRequest request = new PersonalSurveyRequest();

        ApiResponse<Void> response = personalSurveyController.savePersonalSurvey(1L, request);

        verify(personalSurveyService).save(1L, request);
        assertTrue(response.isSuccess());
        assertNull(response.getData());
    }

    /** 로그인 회원의 개인 설문 결과를 서비스에서 조회해 성공 응답으로 감싸 반환하는지 검증한다. */
    @Test
    void returnsPersonalSurveyResultForLoginMember() {
        PersonalSurveyResponse surveyResult =
                new PersonalSurveyResponse("김하나", "안정추구형", "안정성을 우선해요", "투자성향 설명");
        when(personalSurveyService.getPersonalSurveyResult(1L)).thenReturn(surveyResult);

        ApiResponse<PersonalSurveyResponse> response =
                personalSurveyController.getPersonalSurveyResult(1L);

        verify(personalSurveyService).getPersonalSurveyResult(1L);
        assertTrue(response.isSuccess());
        assertSame(surveyResult, response.getData());
    }
}
