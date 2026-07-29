package com.financematch.onboarding.mapper;

import com.financematch.onboarding.dto.OnboardingStatusResponse;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OnboardingMapper {

    OnboardingStatusResponse findOnboardingStatus(Long memberId);
}
