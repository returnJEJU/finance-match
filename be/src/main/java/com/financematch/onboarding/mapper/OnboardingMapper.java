package com.financematch.onboarding.mapper;

import com.financematch.onboarding.dto.OnboardingStatusResponse;

public interface OnboardingMapper {

    OnboardingStatusResponse findOnboardingStatus(Long memberId);
}
