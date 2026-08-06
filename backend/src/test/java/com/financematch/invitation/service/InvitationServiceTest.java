package com.financematch.invitation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.invitation.domain.CommonSurveyUpdateTarget;
import com.financematch.invitation.dto.UpdateCommonSurveyRequest;
import com.financematch.invitation.mapper.InvitationMapper;
import com.financematch.personalsurvey.event.PersonalSurveyCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationMapper invitationMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private InvitationService invitationService;

    @BeforeEach
    void setUp() {
        invitationService = new InvitationService(invitationMapper, eventPublisher);
    }

    @Test
    void updatesCommonSurveyWithoutInvalidatingResultsWhenCoupleIsNotConnected() throws Exception {
        UpdateCommonSurveyRequest request = request();
        CommonSurveyUpdateTarget target = mock(CommonSurveyUpdateTarget.class);
        when(target.getCommonSurveyId()).thenReturn(10L);
        when(target.getCoupleId()).thenReturn(null);
        when(invitationMapper.lockMemberById(1L)).thenReturn(1L);
        when(invitationMapper.findCommonSurveyUpdateTargetForUpdate(1L)).thenReturn(target);
        when(invitationMapper.updateCommonSurvey(10L, request)).thenReturn(1);

        invitationService.updateCommonSurvey(1L, request);

        verify(invitationMapper).updateCommonSurvey(10L, request);
        verify(invitationMapper, never()).deleteRecommendationByCoupleId(any());
        verify(invitationMapper, never()).deleteCompatibilityResultByCoupleId(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void invalidatesResultsAndPublishesEventWhenCoupleIsConnected() throws Exception {
        UpdateCommonSurveyRequest request = request();
        CommonSurveyUpdateTarget target = mock(CommonSurveyUpdateTarget.class);
        when(target.getCommonSurveyId()).thenReturn(10L);
        when(target.getCoupleId()).thenReturn(20L);
        when(invitationMapper.lockMemberById(1L)).thenReturn(1L);
        when(invitationMapper.findCommonSurveyUpdateTargetForUpdate(1L)).thenReturn(target);
        when(invitationMapper.updateCommonSurvey(10L, request)).thenReturn(1);

        invitationService.updateCommonSurvey(1L, request);

        verify(invitationMapper).deleteRecommendationByCoupleId(20L);
        verify(invitationMapper).deleteCompatibilityResultByCoupleId(20L);
        verify(eventPublisher).publishEvent(any(PersonalSurveyCompletedEvent.class));
    }

    @Test
    void throwsWhenAccessibleCommonSurveyDoesNotExist() throws Exception {
        UpdateCommonSurveyRequest request = request();
        when(invitationMapper.lockMemberById(1L)).thenReturn(1L);
        when(invitationMapper.findCommonSurveyUpdateTargetForUpdate(1L)).thenReturn(null);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> invitationService.updateCommonSurvey(1L, request));

        verify(invitationMapper, never()).updateCommonSurvey(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
        assertEquals(ErrorCode.COMMON_SURVEY_NOT_FOUND, exception.getErrorCode());
    }

    private UpdateCommonSurveyRequest request() throws Exception {
        return new ObjectMapper()
                .readValue(
                        """
                        {
                          "goalType1": "INVESTMENT",
                          "goalType2": "RETIREMENT",
                          "targetAmount": 50000000,
                          "targetPeriodMonths": 36,
                          "loanPurpose": "NONE",
                          "hasLoanWithinOneMonth": false
                        }
                        """,
                        UpdateCommonSurveyRequest.class);
    }
}
