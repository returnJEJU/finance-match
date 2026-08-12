package com.financematch.invitation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.invitation.domain.CommonSurvey;
import com.financematch.invitation.domain.CommonSurveyUpdateTarget;
import com.financematch.invitation.dto.CommonSurveyResponse;
import com.financematch.invitation.dto.CreateInvitationRequest;
import com.financematch.invitation.dto.CreateInvitationResponse;
import com.financematch.invitation.dto.GetInvitationResponse;
import com.financematch.invitation.dto.UpdateCommonSurveyRequest;
import com.financematch.invitation.mapper.InvitationMapper;
import com.financematch.personalsurvey.event.PersonalSurveyCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    void createsInvitation() throws Exception {
        CreateInvitationRequest request = createInvitationRequest();
        prepareMemberForInvitation();
        prepareSuccessfulCommonSurveyInsert(10L);
        when(invitationMapper.insertInvitationCode(eq(10L), anyString())).thenReturn(1);

        CreateInvitationResponse response = invitationService.createInvitation(1L, request);

        ArgumentCaptor<CommonSurvey> surveyCaptor = ArgumentCaptor.forClass(CommonSurvey.class);
        verify(invitationMapper).insertCommonSurvey(surveyCaptor.capture());
        CommonSurvey savedSurvey = surveyCaptor.getValue();
        assertEquals(1L, savedSurvey.getMemberId());
        assertEquals(request.getGoalType1(), savedSurvey.getFirstGoalType());
        assertEquals(request.getGoalType2(), savedSurvey.getSecondGoalType());
        assertEquals(request.getTargetAmount(), savedSurvey.getTargetAmount());
        assertEquals(request.getTargetPeriodMonths(), savedSurvey.getTargetPeriodMonths());
        assertEquals(request.getLoanPurpose(), savedSurvey.getLoanPurpose());
        assertEquals(request.getHasLoanWithinOneMonth(), savedSurvey.getHasLoanWithinOneMonth());

        String inviteCode = response.getInviteCode();
        assertNotNull(inviteCode);
        assertEquals(8, inviteCode.length());
        assertTrue(inviteCode.matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}"));
        verify(invitationMapper).insertInvitationCode(10L, inviteCode);
    }

    @Test
    void throwsWhenCreatingInvitationForMissingMember() throws Exception {
        CreateInvitationRequest request = createInvitationRequest();
        when(invitationMapper.lockMemberById(1L)).thenReturn(null);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> invitationService.createInvitation(1L, request));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(invitationMapper, never()).existsCoupleByMemberId(any());
        verify(invitationMapper, never()).insertCommonSurvey(any(CommonSurvey.class));
    }

    @Test
    void throwsWhenMemberIsAlreadyConnected() throws Exception {
        CreateInvitationRequest request = createInvitationRequest();
        when(invitationMapper.lockMemberById(1L)).thenReturn(1L);
        when(invitationMapper.existsCoupleByMemberId(1L)).thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> invitationService.createInvitation(1L, request));

        assertEquals(ErrorCode.COUPLE_ALREADY_CONNECTED, exception.getErrorCode());
        verify(invitationMapper, never()).existsActiveInvitationByMemberId(any());
        verify(invitationMapper, never()).insertCommonSurvey(any(CommonSurvey.class));
    }

    @Test
    void throwsWhenActiveInvitationAlreadyExists() throws Exception {
        CreateInvitationRequest request = createInvitationRequest();
        when(invitationMapper.lockMemberById(1L)).thenReturn(1L);
        when(invitationMapper.existsCoupleByMemberId(1L)).thenReturn(false);
        when(invitationMapper.existsActiveInvitationByMemberId(1L)).thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> invitationService.createInvitation(1L, request));

        assertEquals(ErrorCode.INVITATION_ALREADY_EXISTS, exception.getErrorCode());
        verify(invitationMapper, never()).insertCommonSurvey(any(CommonSurvey.class));
        verify(invitationMapper, never()).insertInvitationCode(any(), anyString());
    }

    @Test
    void throwsWhenCommonSurveyInsertFails() throws Exception {
        CreateInvitationRequest request = createInvitationRequest();
        prepareMemberForInvitation();
        when(invitationMapper.insertCommonSurvey(any(CommonSurvey.class))).thenReturn(0);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> invitationService.createInvitation(1L, request));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verify(invitationMapper, never()).insertInvitationCode(any(), anyString());
    }

    @Test
    void throwsWhenInsertedCommonSurveyHasNoId() throws Exception {
        CreateInvitationRequest request = createInvitationRequest();
        prepareMemberForInvitation();
        when(invitationMapper.insertCommonSurvey(any(CommonSurvey.class))).thenReturn(1);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> invitationService.createInvitation(1L, request));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verify(invitationMapper, never()).insertInvitationCode(any(), anyString());
    }

    @Test
    void retriesWhenGeneratedInvitationCodeIsDuplicated() throws Exception {
        CreateInvitationRequest request = createInvitationRequest();
        prepareMemberForInvitation();
        prepareSuccessfulCommonSurveyInsert(10L);
        when(invitationMapper.insertInvitationCode(eq(10L), anyString())).thenReturn(0, 1); // 첫 번째 호출 0, 두 번째 호출 1

        CreateInvitationResponse response = invitationService.createInvitation(1L, request);

        assertNotNull(response.getInviteCode());
        verify(invitationMapper, times(2)).insertInvitationCode(eq(10L), anyString());
    }

    @Test
    void throwsWhenInvitationCodeGenerationFailsTenTimes() throws Exception {
        CreateInvitationRequest request = createInvitationRequest();
        prepareMemberForInvitation();
        prepareSuccessfulCommonSurveyInsert(10L);
        when(invitationMapper.insertInvitationCode(eq(10L), anyString())).thenReturn(0);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> invitationService.createInvitation(1L, request));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verify(invitationMapper, times(10)).insertInvitationCode(eq(10L), anyString());
    }

    @Test
    void returnsActiveInvitation() {
        when(invitationMapper.findActiveInviteCodeByMemberId(1L)).thenReturn("ABCDEFGH");

        GetInvitationResponse response = invitationService.getInvitation(1L);

        verify(invitationMapper).findActiveInviteCodeByMemberId(1L);
        assertTrue(response.isHasInvitation());
        assertEquals("ABCDEFGH", response.getInviteCode());
    }

    @Test
    void returnsNoInvitationWhenActiveCodeDoesNotExist() {
        when(invitationMapper.findActiveInviteCodeByMemberId(1L)).thenReturn(null);

        GetInvitationResponse response = invitationService.getInvitation(1L);

        verify(invitationMapper).findActiveInviteCodeByMemberId(1L);
        assertFalse(response.isHasInvitation());
        assertNull(response.getInviteCode());
    }

    @Test
    void returnsCommonSurveyWhenAccessibleSurveyExists() {
        CommonSurveyResponse expected = new CommonSurveyResponse();
        when(invitationMapper.findCommonSurveyByAccessibleMemberId(1L)).thenReturn(expected);

        CommonSurveyResponse actual = invitationService.getCommonSurvey(1L);

        verify(invitationMapper).findCommonSurveyByAccessibleMemberId(1L);
        assertSame(expected, actual);
    }

    @Test
    void throwsWhenGettingCommonSurveyThatDoesNotExist() {
        when(invitationMapper.findCommonSurveyByAccessibleMemberId(1L)).thenReturn(null);

        ApiException exception =
                assertThrows(ApiException.class, () -> invitationService.getCommonSurvey(1L));

        verify(invitationMapper).findCommonSurveyByAccessibleMemberId(1L);
        assertEquals(ErrorCode.COMMON_SURVEY_NOT_FOUND, exception.getErrorCode());
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
    void throwsWhenMemberDoesNotExist() throws Exception {
        UpdateCommonSurveyRequest request = request();
        when(invitationMapper.lockMemberById(1L)).thenReturn(null);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> invitationService.updateCommonSurvey(1L, request));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(invitationMapper, never()).findCommonSurveyUpdateTargetForUpdate(any());
        verify(invitationMapper, never()).updateCommonSurvey(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
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

    @Test
    void throwsWhenCommonSurveyUpdateFails() throws Exception {
        UpdateCommonSurveyRequest request = request();
        CommonSurveyUpdateTarget target = mock(CommonSurveyUpdateTarget.class);
        when(target.getCommonSurveyId()).thenReturn(10L);
        when(invitationMapper.lockMemberById(1L)).thenReturn(1L);
        when(invitationMapper.findCommonSurveyUpdateTargetForUpdate(1L)).thenReturn(target);
        when(invitationMapper.updateCommonSurvey(10L, request)).thenReturn(0);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> invitationService.updateCommonSurvey(1L, request));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verify(invitationMapper).updateCommonSurvey(10L, request);
        verify(invitationMapper, never()).deleteRecommendationByCoupleId(any());
        verify(invitationMapper, never()).deleteCompatibilityResultByCoupleId(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private void prepareMemberForInvitation() {
        when(invitationMapper.lockMemberById(1L)).thenReturn(1L);
        when(invitationMapper.existsCoupleByMemberId(1L)).thenReturn(false);
        when(invitationMapper.existsActiveInvitationByMemberId(1L)).thenReturn(false);
    }

    private void prepareSuccessfulCommonSurveyInsert(Long commonSurveyId) {
        when(invitationMapper.insertCommonSurvey(any(CommonSurvey.class)))
                .thenAnswer(
                        invocation -> {
                            CommonSurvey commonSurvey = invocation.getArgument(0);
                            commonSurvey.setId(commonSurveyId);
                            return 1;
                        });
    }

    private CreateInvitationRequest createInvitationRequest() throws Exception {
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
                        CreateInvitationRequest.class);
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
