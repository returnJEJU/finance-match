package com.financematch.couple.service;

import com.financematch.common.ErrorCode;
import com.financematch.couple.domain.CoupleDisconnectTarget;
import com.financematch.couple.domain.InvitationTarget;
import com.financematch.couple.domain.CoupleProfile;
import com.financematch.couple.dto.CoupleProfileMessageResponse;
import com.financematch.couple.dto.CreateCoupleRequest;
import com.financematch.couple.dto.CreateCoupleResponse;
import com.financematch.couple.mapper.CoupleMapper;
import com.financematch.exception.ApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoupleService {

    private static final String DEFAULT_PROFILE_MESSAGE = "우리의 금융 여정";

    private final CoupleMapper coupleMapper;

    @Transactional(readOnly = true)
    public CoupleProfileMessageResponse getProfileMessage(Long memberId) {
        CoupleProfile profile = findCoupleProfile(memberId);

        // DB에 아직 값이 없으면 기존 프론트 기본 문구를 응답한다.
        String profileMessage =
                profile.getProfileMessage() != null
                        ? profile.getProfileMessage()
                        : DEFAULT_PROFILE_MESSAGE;

        return new CoupleProfileMessageResponse(
                profile.getMyName(),
                profile.getPartnerName(),
                profileMessage);
    }

    @Transactional
    public CoupleProfileMessageResponse updateProfileMessage(
            Long memberId,
            String profileMessage) {
        CoupleProfile profile = findCoupleProfile(memberId);

        int updatedRows =
                coupleMapper.updateProfileMessageByMemberId(
                        memberId,
                        profileMessage);

        if (updatedRows != 1) {
            throw new ApiException(ErrorCode.COUPLE_NOT_CONNECTED);
        }

        return new CoupleProfileMessageResponse(
                profile.getMyName(),
                profile.getPartnerName(),
                profileMessage);
    }

    @Transactional
    public void disconnectCouple(Long memberId) {
        if (memberId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }

        CoupleDisconnectTarget target =
                coupleMapper.findDisconnectTargetByMemberId(memberId);

        if (target == null) {
            throw new ApiException(ErrorCode.COUPLE_NOT_CONNECTED);
        }

        disconnectCouple(target, memberId);
    }

    @Transactional
    public void disconnectCoupleIfConnected(Long memberId) {
        if (memberId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }

        CoupleDisconnectTarget target =
                coupleMapper.findDisconnectTargetByMemberId(memberId);

        if (target == null) {
            return;
        }

        disconnectCouple(target, memberId);
    }

    private void disconnectCouple(CoupleDisconnectTarget target, Long memberId) {
        // 개인 추천은 member_id 기준 테이블이라 couple 삭제 cascade 대상이 아니다.
        coupleMapper.deletePersonalTaxSavingByCoupleMembers(
                target.getInviterId(),
                target.getInviteeId());
        coupleMapper.deletePersonalInvestmentByCoupleMembers(
                target.getInviterId(),
                target.getInviteeId());

        // couple 삭제 시 compatibility_result/report/recommendation 계열은 FK cascade로 함께 삭제된다.
        int deletedCoupleRows =
                coupleMapper.deleteCoupleByIdAndMemberId(
                        target.getCoupleId(),
                        memberId);

        if (deletedCoupleRows != 1) {
            throw new ApiException(ErrorCode.COUPLE_NOT_CONNECTED);
        }

        // 재연결 시 새 공동설문과 초대코드를 만들 수 있도록 기존 연결 원천 데이터를 제거한다.
        int deletedInvitationRows =
                coupleMapper.deleteInvitationById(target.getInvitationCodeId());
        int deletedCommonSurveyRows =
                coupleMapper.deleteCommonSurveyById(target.getCommonSurveyId());

        if (deletedInvitationRows != 1 || deletedCommonSurveyRows != 1) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private CoupleProfile findCoupleProfile(Long memberId) {
        CoupleProfile profile = coupleMapper.findProfileByMemberId(memberId);
        if (profile == null) {
            throw new ApiException(ErrorCode.COUPLE_NOT_CONNECTED);
        }
        return profile;
    }

    @Transactional
    public CreateCoupleResponse createCouple(Long memberId, CreateCoupleRequest request) {

        // 초대코드 조회 및 검증
        InvitationTarget target = coupleMapper.findInvitationTargetForUpdate(request.getInviteCode());
        if (target == null) {
            throw new ApiException(ErrorCode.INVITATION_NOT_FOUND);
        }

        // 자기 자신과 커플 연동 불가
        Long inviterId = target.getInviterId();
        if (memberId.equals(inviterId)) {
            throw new ApiException(ErrorCode.SELF_INVITATION_NOT_ALLOWED);
        }

        // A, B 회원 잠금 + 검증
        Long firstMemberId = Math.min(memberId, inviterId);
        Long secondMemberId = Math.max(memberId, inviterId);
        List<Long> lockedMemberIds = coupleMapper.lockMembersForUpdate(firstMemberId, secondMemberId);
        if (!lockedMemberIds.contains(memberId)) {
            // 현재 사용자(초대코드 입력자) A가 유효하지 않음
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        if (!lockedMemberIds.contains(inviterId)) {
            // 초대코드 생성자가 유효하지 않음 -> 사용할 수 없는 초대 코드
            throw new ApiException(ErrorCode.INVITATION_NOT_AVAILABLE);
        }

        // A 커플 여부 검증
        if (coupleMapper.existsCoupleByMemberId(memberId)) {
            throw new ApiException(ErrorCode.COUPLE_ALREADY_CONNECTED);
        }

        // 초대코드 ACTIVE 여부 및 B 커플 여부 검증
        if (!"ACTIVE".equals(target.getStatus())
                || coupleMapper.existsCoupleByMemberId(inviterId)) {
            throw new ApiException(ErrorCode.INVITATION_NOT_AVAILABLE);
        }

        // A의 정보 삭제
        coupleMapper.deleteInvitationByMemberId(memberId);
        coupleMapper.deleteCommonSurveyByMemberId(memberId);
        coupleMapper.deleteInvestmentExperiencesByMemberId(memberId);
        coupleMapper.deletePersonalSurveyByMemberId(memberId);
        coupleMapper.clearInvestmentType(memberId);

        // 커플 생성
        Long invitationCodeId = target.getInvitationCodeId();
        int insertedRows = coupleMapper.insertCouple(inviterId, memberId, invitationCodeId);
        if (insertedRows != 1) {
            // 예상치 못한 커플 생성 실패
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }

        // 초대 코드 사용 표시
        int updatedRows = coupleMapper.markInvitationUsed(target.getInvitationCodeId());
        if (updatedRows != 1) {
            // 사용할 수 없는 초대 코드 (상태가 ACTIVE가 아님)
            throw new ApiException(ErrorCode.INVITATION_NOT_AVAILABLE);
        }

        // 파트너 이름
        String partnerName = coupleMapper.findMemberNameById(inviterId);

        return new CreateCoupleResponse(partnerName);
    }
}
