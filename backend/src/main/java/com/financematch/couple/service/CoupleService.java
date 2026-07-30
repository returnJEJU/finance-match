package com.financematch.couple.service;

import com.financematch.common.ErrorCode;
import com.financematch.couple.domain.CoupleProfile;
import com.financematch.couple.dto.CoupleProfileMessageResponse;
import com.financematch.couple.mapper.CoupleMapper;
import com.financematch.exception.ApiException;
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

        return new CoupleProfileMessageResponse(profileMessage);
    }

    @Transactional
    public CoupleProfileMessageResponse updateProfileMessage(
            Long memberId,
            String profileMessage) {
        findCoupleProfile(memberId);

        int updatedRows =
                coupleMapper.updateProfileMessageByMemberId(
                        memberId,
                        profileMessage);

        if (updatedRows != 1) {
            throw new ApiException(ErrorCode.COUPLE_NOT_CONNECTED);
        }

        return new CoupleProfileMessageResponse(profileMessage);
    }

    private CoupleProfile findCoupleProfile(Long memberId) {
        CoupleProfile profile = coupleMapper.findProfileByMemberId(memberId);
        if (profile == null) {
            throw new ApiException(ErrorCode.COUPLE_NOT_CONNECTED);
        }
        return profile;
    }
}
