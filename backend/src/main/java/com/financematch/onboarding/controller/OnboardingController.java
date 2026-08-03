package com.financematch.onboarding.controller;

import com.financematch.auth.annotation.LoginMember;
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

    private final OnboardingService onboardingService;

    @GetMapping("")
    public ApiResponse<OnboardingStatusResponse> getOnboardingStatus(
            @LoginMember Long memberId) {

        return ApiResponse.ok(onboardingService.getOnboardingStatus(memberId));
    }
}
