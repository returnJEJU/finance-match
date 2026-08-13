package com.financematch.couple.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.common.ErrorCode;
import com.financematch.couple.domain.CoupleDisconnectTarget;
import com.financematch.couple.domain.CoupleProfile;
import com.financematch.couple.domain.InvitationTarget;
import com.financematch.couple.dto.CoupleProfileMessageResponse;
import com.financematch.couple.dto.CreateCoupleRequest;
import com.financematch.couple.dto.CreateCoupleResponse;
import com.financematch.couple.mapper.CoupleMapper;
import com.financematch.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoupleServiceUnitTest {

    private static final Long MEMBER_ID = 2L;
    private static final Long INVITER_ID = 1L;
    private static final Long COUPLE_ID = 10L;
    private static final Long INVITATION_CODE_ID = 20L;
    private static final Long COMMON_SURVEY_ID = 30L;
    private static final String INVITE_CODE = "ABCDEFGH";

    @Mock private CoupleMapper coupleMapper;

    @InjectMocks private CoupleService coupleService;

    /** 저장된 커플 이름과 한줄 소개를 응답으로 반환하는지 검증한다. */
    @Test
    void returnsCoupleProfileMessage() {
        CoupleProfile profile = mock(CoupleProfile.class);
        when(profile.getMyName()).thenReturn("이두리");
        when(profile.getPartnerName()).thenReturn("김하나");
        when(profile.getProfileMessage()).thenReturn("함께 부자 되기");
        when(coupleMapper.findProfileByMemberId(MEMBER_ID)).thenReturn(profile);

        CoupleProfileMessageResponse response = coupleService.getProfileMessage(MEMBER_ID);

        verify(coupleMapper).findProfileByMemberId(MEMBER_ID);
        assertEquals("이두리", response.getMyName());
        assertEquals("김하나", response.getPartnerName());
        assertEquals("함께 부자 되기", response.getProfileMessage());
    }

    /** 저장된 한줄 소개가 없으면 기본 문구를 반환하는지 검증한다. */
    @Test
    void returnsDefaultProfileMessageWhenStoredMessageIsNull() {
        CoupleProfile profile = mock(CoupleProfile.class);
        when(profile.getMyName()).thenReturn("이두리");
        when(profile.getPartnerName()).thenReturn("김하나");
        when(profile.getProfileMessage()).thenReturn(null);
        when(coupleMapper.findProfileByMemberId(MEMBER_ID)).thenReturn(profile);

        CoupleProfileMessageResponse response = coupleService.getProfileMessage(MEMBER_ID);

        assertEquals("이두리", response.getMyName());
        assertEquals("김하나", response.getPartnerName());
        assertEquals("우리의 금융 여정", response.getProfileMessage());
    }

    /** 연결된 커플 프로필이 없으면 COUPLE_NOT_CONNECTED 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenGettingProfileWithoutConnectedCouple() {
        when(coupleMapper.findProfileByMemberId(MEMBER_ID)).thenReturn(null);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.getProfileMessage(MEMBER_ID));

        assertEquals(ErrorCode.COUPLE_NOT_CONNECTED, exception.getErrorCode());
    }

    /** 한줄 소개를 수정하고 두 회원의 이름과 수정된 문구를 반환하는지 검증한다. */
    @Test
    void updatesAndReturnsCoupleProfileMessage() {
        CoupleProfile profile = mock(CoupleProfile.class);
        when(profile.getMyName()).thenReturn("이두리");
        when(profile.getPartnerName()).thenReturn("김하나");
        when(coupleMapper.findProfileByMemberId(MEMBER_ID)).thenReturn(profile);
        when(coupleMapper.updateProfileMessageByMemberId(MEMBER_ID, "함께 부자 되기"))
                .thenReturn(1);

        CoupleProfileMessageResponse response =
                coupleService.updateProfileMessage(MEMBER_ID, "함께 부자 되기");

        verify(coupleMapper).updateProfileMessageByMemberId(MEMBER_ID, "함께 부자 되기");
        assertEquals("이두리", response.getMyName());
        assertEquals("김하나", response.getPartnerName());
        assertEquals("함께 부자 되기", response.getProfileMessage());
    }

    /** 한줄 소개 수정 결과가 1건이 아니면 COUPLE_NOT_CONNECTED 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenProfileMessageUpdateFails() {
        CoupleProfile profile = mock(CoupleProfile.class);
        when(coupleMapper.findProfileByMemberId(MEMBER_ID)).thenReturn(profile);
        when(coupleMapper.updateProfileMessageByMemberId(MEMBER_ID, "함께 부자 되기"))
                .thenReturn(0);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                coupleService.updateProfileMessage(
                                        MEMBER_ID,
                                        "함께 부자 되기"));

        assertEquals(ErrorCode.COUPLE_NOT_CONNECTED, exception.getErrorCode());
    }

    /** 커플 해제 요청의 회원 ID가 null이면 INVALID_INPUT 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenDisconnectMemberIdIsNull() {
        ApiException exception =
                assertThrows(ApiException.class, () -> coupleService.disconnectCouple(null));

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verifyNoInteractions(coupleMapper);
    }

    /** 해제할 커플이 없으면 COUPLE_NOT_CONNECTED 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenDisconnectingWithoutConnectedCouple() {
        when(coupleMapper.findDisconnectTargetByMemberId(MEMBER_ID)).thenReturn(null);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.disconnectCouple(MEMBER_ID));

        assertEquals(ErrorCode.COUPLE_NOT_CONNECTED, exception.getErrorCode());
        verify(coupleMapper, never()).deleteCoupleByIdAndMemberId(any(), any());
    }

    /** 커플 행 삭제에 실패하면 후속 데이터를 삭제하지 않고 예외를 발생시키는지 검증한다. */
    @Test
    void throwsWhenCoupleDeletionFails() {
        CoupleDisconnectTarget target = mock(CoupleDisconnectTarget.class);
        when(target.getCoupleId()).thenReturn(COUPLE_ID);
        when(target.getInviterId()).thenReturn(INVITER_ID);
        when(target.getInviteeId()).thenReturn(MEMBER_ID);
        when(coupleMapper.findDisconnectTargetByMemberId(MEMBER_ID)).thenReturn(target);
        when(coupleMapper.deleteCoupleByIdAndMemberId(COUPLE_ID, MEMBER_ID)).thenReturn(0);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.disconnectCouple(MEMBER_ID));

        assertEquals(ErrorCode.COUPLE_NOT_CONNECTED, exception.getErrorCode());
        verify(coupleMapper, never()).deleteInvitationById(any());
        verify(coupleMapper, never()).deleteCommonSurveyById(any());
        verify(coupleMapper, never()).deleteInvestmentExperiencesByMemberId(any());
        verify(coupleMapper, never()).deletePersonalSurveyByMemberId(any());
        verify(coupleMapper, never()).clearInvestmentType(any());
    }

    /** 커플 생성에 사용된 초대 코드 삭제에 실패하면 INTERNAL_ERROR가 발생하는지 검증한다. */
    @Test
    void throwsWhenInvitationDeletionFails() {
        CoupleDisconnectTarget target = disconnectTarget();
        when(coupleMapper.findDisconnectTargetByMemberId(MEMBER_ID)).thenReturn(target);
        when(coupleMapper.deleteCoupleByIdAndMemberId(COUPLE_ID, MEMBER_ID)).thenReturn(1);
        when(coupleMapper.deleteInvitationById(INVITATION_CODE_ID)).thenReturn(0);
        when(coupleMapper.deleteCommonSurveyById(COMMON_SURVEY_ID)).thenReturn(1);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.disconnectCouple(MEMBER_ID));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verify(coupleMapper).deleteInvitationById(INVITATION_CODE_ID);
        verify(coupleMapper).deleteCommonSurveyById(COMMON_SURVEY_ID);
    }

    /** 커플 생성에 사용된 공동 설문 삭제에 실패하면 INTERNAL_ERROR가 발생하는지 검증한다. */
    @Test
    void throwsWhenCommonSurveyDeletionFails() {
        CoupleDisconnectTarget target = disconnectTarget();
        when(coupleMapper.findDisconnectTargetByMemberId(MEMBER_ID)).thenReturn(target);
        when(coupleMapper.deleteCoupleByIdAndMemberId(COUPLE_ID, MEMBER_ID)).thenReturn(1);
        when(coupleMapper.deleteInvitationById(INVITATION_CODE_ID)).thenReturn(1);
        when(coupleMapper.deleteCommonSurveyById(COMMON_SURVEY_ID)).thenReturn(0);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.disconnectCouple(MEMBER_ID));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
    }

    /** 탈퇴용 커플 해제에서도 회원 ID가 null이면 INVALID_INPUT 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenOptionalDisconnectMemberIdIsNull() {
        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.disconnectCoupleIfConnected(null));

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verifyNoInteractions(coupleMapper);
    }

    /** 탈퇴 회원에게 연결된 커플이 없으면 삭제 없이 정상 종료하는지 검증한다. */
    @Test
    void doesNothingWhenOptionalDisconnectHasNoCouple() {
        when(coupleMapper.findDisconnectTargetByMemberId(MEMBER_ID)).thenReturn(null);

        assertDoesNotThrow(() -> coupleService.disconnectCoupleIfConnected(MEMBER_ID));

        verify(coupleMapper, never()).deleteCoupleByIdAndMemberId(any(), any());
    }

    /** 탈퇴 회원에게 연결된 커플이 있으면 관련 데이터를 모두 삭제하는지 검증한다. */
    @Test
    void disconnectsWhenOptionalDisconnectHasCouple() {
        CoupleDisconnectTarget target = disconnectTarget();
        when(coupleMapper.findDisconnectTargetByMemberId(MEMBER_ID)).thenReturn(target);
        when(coupleMapper.deleteCoupleByIdAndMemberId(COUPLE_ID, MEMBER_ID)).thenReturn(1);
        when(coupleMapper.deleteInvitationById(INVITATION_CODE_ID)).thenReturn(1);
        when(coupleMapper.deleteCommonSurveyById(COMMON_SURVEY_ID)).thenReturn(1);

        coupleService.disconnectCoupleIfConnected(MEMBER_ID);

        verify(coupleMapper).deletePersonalTaxSavingByCoupleMembers(INVITER_ID, MEMBER_ID);
        verify(coupleMapper).deletePersonalInvestmentByCoupleMembers(INVITER_ID, MEMBER_ID);
        verify(coupleMapper).deleteCoupleByIdAndMemberId(COUPLE_ID, MEMBER_ID);

        verify(coupleMapper).deleteInvestmentExperiencesByMemberId(INVITER_ID);
        verify(coupleMapper).deletePersonalSurveyByMemberId(INVITER_ID);
        verify(coupleMapper).clearInvestmentType(INVITER_ID);

        verify(coupleMapper).deleteInvestmentExperiencesByMemberId(MEMBER_ID);
        verify(coupleMapper).deletePersonalSurveyByMemberId(MEMBER_ID);
        verify(coupleMapper).clearInvestmentType(MEMBER_ID);

        verify(coupleMapper).deleteInvitationById(INVITATION_CODE_ID);
        verify(coupleMapper).deleteCommonSurveyById(COMMON_SURVEY_ID);
    }

    /** 유효한 초대 코드로 커플을 생성하고 파트너 이름을 반환하는 전체 흐름을 검증한다. */
    @Test
    void createsCouple() throws Exception {
        CreateCoupleRequest request = createCoupleRequest();
        prepareAvailableInvitation();
        when(coupleMapper.insertCouple(INVITER_ID, MEMBER_ID, INVITATION_CODE_ID)).thenReturn(1);
        when(coupleMapper.markInvitationUsed(INVITATION_CODE_ID)).thenReturn(1);
        when(coupleMapper.findMemberNameById(INVITER_ID)).thenReturn("김하나");

        CreateCoupleResponse response = coupleService.createCouple(MEMBER_ID, request);

        verify(coupleMapper).lockMembersForUpdate(INVITER_ID, MEMBER_ID);
        verify(coupleMapper).deleteInvitationByMemberId(MEMBER_ID);
        verify(coupleMapper).deleteCommonSurveyByMemberId(MEMBER_ID);
        verify(coupleMapper).deleteInvestmentExperiencesByMemberId(MEMBER_ID);
        verify(coupleMapper).deletePersonalSurveyByMemberId(MEMBER_ID);
        verify(coupleMapper).clearInvestmentType(MEMBER_ID);
        verify(coupleMapper).insertCouple(INVITER_ID, MEMBER_ID, INVITATION_CODE_ID);
        verify(coupleMapper).markInvitationUsed(INVITATION_CODE_ID);
        assertEquals("김하나", response.getPartnerName());
    }

    /** 입력한 초대 코드를 찾지 못하면 INVITATION_NOT_FOUND 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenInvitationDoesNotExist() throws Exception {
        CreateCoupleRequest request = createCoupleRequest();
        when(coupleMapper.findInvitationTargetForUpdate(INVITE_CODE)).thenReturn(null);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.createCouple(MEMBER_ID, request));

        assertEquals(ErrorCode.INVITATION_NOT_FOUND, exception.getErrorCode());
        verify(coupleMapper, never()).lockMembersForUpdate(any(), any());
    }

    /** 자신이 만든 초대 코드를 사용하면 SELF_INVITATION_NOT_ALLOWED 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenMemberUsesOwnInvitation() throws Exception {
        CreateCoupleRequest request = createCoupleRequest();
        InvitationTarget target = invitationTarget(MEMBER_ID, "ACTIVE");
        when(coupleMapper.findInvitationTargetForUpdate(INVITE_CODE)).thenReturn(target);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.createCouple(MEMBER_ID, request));

        assertEquals(ErrorCode.SELF_INVITATION_NOT_ALLOWED, exception.getErrorCode());
        verify(coupleMapper, never()).lockMembersForUpdate(any(), any());
    }

    /** 현재 회원을 잠금 조회하지 못하면 UNAUTHORIZED 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenCurrentMemberCannotBeLocked() throws Exception {
        CreateCoupleRequest request = createCoupleRequest();
        InvitationTarget target = invitationTarget(INVITER_ID, "ACTIVE");
        when(coupleMapper.findInvitationTargetForUpdate(INVITE_CODE)).thenReturn(target);
        when(coupleMapper.lockMembersForUpdate(INVITER_ID, MEMBER_ID))
                .thenReturn(List.of(INVITER_ID));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.createCouple(MEMBER_ID, request));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verify(coupleMapper, never()).existsCoupleByMemberId(any());
    }

    /** 초대 코드 생성자를 잠금 조회하지 못하면 INVITATION_NOT_AVAILABLE 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenInviterCannotBeLocked() throws Exception {
        CreateCoupleRequest request = createCoupleRequest();
        InvitationTarget target = invitationTarget(INVITER_ID, "ACTIVE");
        when(coupleMapper.findInvitationTargetForUpdate(INVITE_CODE)).thenReturn(target);
        when(coupleMapper.lockMembersForUpdate(INVITER_ID, MEMBER_ID))
                .thenReturn(List.of(MEMBER_ID));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.createCouple(MEMBER_ID, request));

        assertEquals(ErrorCode.INVITATION_NOT_AVAILABLE, exception.getErrorCode());
        verify(coupleMapper, never()).existsCoupleByMemberId(any());
    }

    /** 현재 회원이 이미 커플이면 COUPLE_ALREADY_CONNECTED 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenCurrentMemberAlreadyConnected() throws Exception {
        CreateCoupleRequest request = createCoupleRequest();
        InvitationTarget target = invitationTarget(INVITER_ID, "ACTIVE");
        when(coupleMapper.findInvitationTargetForUpdate(INVITE_CODE)).thenReturn(target);
        when(coupleMapper.lockMembersForUpdate(INVITER_ID, MEMBER_ID))
                .thenReturn(List.of(INVITER_ID, MEMBER_ID));
        when(coupleMapper.existsCoupleByMemberId(MEMBER_ID)).thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.createCouple(MEMBER_ID, request));

        assertEquals(ErrorCode.COUPLE_ALREADY_CONNECTED, exception.getErrorCode());
        verify(coupleMapper, never()).deleteInvitationByMemberId(any());
    }

    /** 초대 코드가 ACTIVE 상태가 아니면 INVITATION_NOT_AVAILABLE 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenInvitationIsNotActive() throws Exception {
        CreateCoupleRequest request = createCoupleRequest();
        InvitationTarget target = invitationTarget(INVITER_ID, "USED");
        when(coupleMapper.findInvitationTargetForUpdate(INVITE_CODE)).thenReturn(target);
        when(coupleMapper.lockMembersForUpdate(INVITER_ID, MEMBER_ID))
                .thenReturn(List.of(INVITER_ID, MEMBER_ID));
        when(coupleMapper.existsCoupleByMemberId(MEMBER_ID)).thenReturn(false);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.createCouple(MEMBER_ID, request));

        assertEquals(ErrorCode.INVITATION_NOT_AVAILABLE, exception.getErrorCode());
        verify(coupleMapper, never()).existsCoupleByMemberId(INVITER_ID);
        verify(coupleMapper, never()).deleteInvitationByMemberId(any());
    }

    /** 초대 코드 생성자가 이미 커플이면 INVITATION_NOT_AVAILABLE 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenInviterAlreadyConnected() throws Exception {
        CreateCoupleRequest request = createCoupleRequest();
        InvitationTarget target = invitationTarget(INVITER_ID, "ACTIVE");
        when(coupleMapper.findInvitationTargetForUpdate(INVITE_CODE)).thenReturn(target);
        when(coupleMapper.lockMembersForUpdate(INVITER_ID, MEMBER_ID))
                .thenReturn(List.of(INVITER_ID, MEMBER_ID));
        when(coupleMapper.existsCoupleByMemberId(MEMBER_ID)).thenReturn(false);
        when(coupleMapper.existsCoupleByMemberId(INVITER_ID)).thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.createCouple(MEMBER_ID, request));

        assertEquals(ErrorCode.INVITATION_NOT_AVAILABLE, exception.getErrorCode());
        verify(coupleMapper, never()).deleteInvitationByMemberId(any());
    }

    /** 커플 저장에 실패하면 INTERNAL_ERROR가 발생하고 초대 코드를 사용 처리하지 않는지 검증한다. */
    @Test
    void throwsWhenCoupleInsertFails() throws Exception {
        CreateCoupleRequest request = createCoupleRequest();
        prepareAvailableInvitation();
        when(coupleMapper.insertCouple(INVITER_ID, MEMBER_ID, INVITATION_CODE_ID)).thenReturn(0);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.createCouple(MEMBER_ID, request));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verify(coupleMapper, never()).markInvitationUsed(any());
    }

    /** 초대 코드 사용 처리에 실패하면 INVITATION_NOT_AVAILABLE 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenMarkingInvitationUsedFails() throws Exception {
        CreateCoupleRequest request = createCoupleRequest();
        prepareAvailableInvitation();
        when(coupleMapper.insertCouple(INVITER_ID, MEMBER_ID, INVITATION_CODE_ID)).thenReturn(1);
        when(coupleMapper.markInvitationUsed(INVITATION_CODE_ID)).thenReturn(0);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.createCouple(MEMBER_ID, request));

        assertEquals(ErrorCode.INVITATION_NOT_AVAILABLE, exception.getErrorCode());
        verify(coupleMapper, never()).findMemberNameById(any());
    }

    private CoupleDisconnectTarget disconnectTarget() {
        CoupleDisconnectTarget target = mock(CoupleDisconnectTarget.class);
        when(target.getCoupleId()).thenReturn(COUPLE_ID);
        when(target.getInviterId()).thenReturn(INVITER_ID);
        when(target.getInviteeId()).thenReturn(MEMBER_ID);
        when(target.getInvitationCodeId()).thenReturn(INVITATION_CODE_ID);
        when(target.getCommonSurveyId()).thenReturn(COMMON_SURVEY_ID);
        return target;
    }

    private void prepareAvailableInvitation() {
        InvitationTarget target = invitationTarget(INVITER_ID, "ACTIVE");
        when(coupleMapper.findInvitationTargetForUpdate(INVITE_CODE)).thenReturn(target);
        when(coupleMapper.lockMembersForUpdate(INVITER_ID, MEMBER_ID))
                .thenReturn(List.of(INVITER_ID, MEMBER_ID));
        when(coupleMapper.existsCoupleByMemberId(MEMBER_ID)).thenReturn(false);
        when(coupleMapper.existsCoupleByMemberId(INVITER_ID)).thenReturn(false);
    }

    private InvitationTarget invitationTarget(Long inviterId, String status) {
        InvitationTarget target = new InvitationTarget();
        target.setInvitationCodeId(INVITATION_CODE_ID);
        target.setCommonSurveyId(COMMON_SURVEY_ID);
        target.setInviterId(inviterId);
        target.setStatus(status);
        return target;
    }

    private CreateCoupleRequest createCoupleRequest() throws Exception {
        return new ObjectMapper()
                .readValue(
                        """
                        { "inviteCode": "ABCDEFGH" }
                        """,
                        CreateCoupleRequest.class);
    }
}
