package com.financematch.onboarding.service;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.onboarding.dto.OnboardingStatusResponse;
import com.financematch.onboarding.mapper.OnboardingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OnboardingMapper onboardingMapper;

    public OnboardingStatusResponse getOnboardingStatus(Long memberId) {
        OnboardingStatusResponse response = onboardingMapper.findOnboardingStatus(memberId);

        // 인증 구현 전 임시ID 사용에 대비한 null 검사
        if (response == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }

        return response;
    }

}
