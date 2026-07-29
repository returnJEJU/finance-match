package com.financematch.invitation.service;


import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.invitation.domain.CommonSurvey;
import com.financematch.invitation.dto.CreateInvitationRequest;
import com.financematch.invitation.dto.CreateInvitationResponse;
import com.financematch.invitation.mapper.InvitationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final String INVITE_CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 혼동 문자 0, O, 1, I 제외
    private static final int INVITE_CODE_LENGTH = 8;
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final InvitationMapper invitationMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public CreateInvitationResponse createInvitation(Long memberId, CreateInvitationRequest request) {

        // 행 잠금 획득 (잠금 해제 조건: 트랜잭션 커밋 또는 롤백)
        Long lockedMemberId = invitationMapper.lockMemberById(memberId);

        // 유효한 회원인지 확인
        if (lockedMemberId == null) {
            // TODO: JWT 구현 후 인증 계층에서 UNAUTHORIZED를 처리한다.
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        // 이미 커플 연동이 완료된 경우
        if (invitationMapper.existsCoupleByMemberId(memberId)) {
            throw new ApiException(ErrorCode.COUPLE_ALREADY_CONNECTED);
        }

        // 이미 활성 초대 코드가 있는 경우
        if (invitationMapper.existsActiveInvitationByMemberId(memberId)) {
            throw new ApiException(ErrorCode.INVITATION_ALREADY_EXISTS);
        }

        // 공동 설문 저장
        CommonSurvey commonSurvey = CommonSurvey.of(memberId, request);
        int insertedSurveyRows = invitationMapper.insertCommonSurvey(commonSurvey);
        if (insertedSurveyRows != 1 || commonSurvey.getId() == null) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }

        // 초대 코드 저장
        String inviteCode = insertInvitationCodeWithRetry(commonSurvey.getId());

        return new CreateInvitationResponse(inviteCode);
    }

    private String insertInvitationCodeWithRetry(Long commonSurveyId) {

        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String inviteCode = generateInviteCode();
            int insertedRows = invitationMapper.insertInvitationCode(commonSurveyId, inviteCode);

            // 코드가 중복이 아니면 삽입 성공 -> insertedRows = 1
            // 코드가 중복이면 삽입 생략 (예외 발생X) -> insertedRows = 0 -> 재시도
            if (insertedRows == 1) return inviteCode;
        }

        throw new ApiException(ErrorCode.INTERNAL_ERROR);
    }

    private String generateInviteCode() {
        StringBuilder inviteCode = new StringBuilder(INVITE_CODE_LENGTH);

        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            // secureRandom.nextInt(n): 0 이상 n 미만의 안전한 무작위 정수 반환
            int index = secureRandom.nextInt(INVITE_CODE_CHARACTERS.length());
            inviteCode.append(INVITE_CODE_CHARACTERS.charAt(index));
        }

        return inviteCode.toString();
    }
}
