package com.financematch.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.common.ApiResponse;
import com.financematch.onboarding.dto.OnboardingStatusResponse;
import com.financematch.onboarding.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {

    @Mock private OnboardingService onboardingService;

    @InjectMocks private OnboardingController onboardingController;

    @Test
    void 인증된_회원의_온보딩_상태를_감싸_반환한다() {
        OnboardingStatusResponse onboardingStatus = new OnboardingStatusResponse();
        when(onboardingService.getOnboardingStatus(1L)).thenReturn(onboardingStatus);

        ApiResponse<OnboardingStatusResponse> response =
                onboardingController.getOnboardingStatus(1L);

        verify(onboardingService).getOnboardingStatus(1L);
        assertTrue(response.isSuccess());
        assertSame(onboardingStatus, response.getData());
    }
}
