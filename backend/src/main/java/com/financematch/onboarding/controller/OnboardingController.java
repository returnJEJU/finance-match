package com.financematch.onboarding.controller;

import com.financematch.common.ApiResponse;
import com.financematch.onboarding.dto.OnboardingStatusResponse;
import com.financematch.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/members/me/onboarding-status")
@RequiredArgsConstructor
public class OnboardingController {

    // 임시 ID
    private static final Long TEMP_MEMBER_ID = 1L;

    private final OnboardingService onboardingService;

    @GetMapping("")
    public ApiResponse<OnboardingStatusResponse> getOnboardingStatus() {
        // JWT 인증 구현 후 임시 ID 대신 인증된 회원 ID를 주입받음
        // 예: getOnboardingStatus(@LoginMember Long memberId)
        Long memberId = TEMP_MEMBER_ID;

        return ApiResponse.ok(onboardingService.getOnboardingStatus(memberId));
    }
}
