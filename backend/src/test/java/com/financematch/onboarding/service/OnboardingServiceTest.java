package com.financematch.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.onboarding.dto.OnboardingStatusResponse;
import com.financematch.onboarding.mapper.OnboardingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock private OnboardingMapper onboardingMapper;

    @InjectMocks private OnboardingService onboardingService;

    @Test
    void 온보딩_상태를_조회해_반환한다() {
        OnboardingStatusResponse expected = new OnboardingStatusResponse();
        when(onboardingMapper.findOnboardingStatus(1L)).thenReturn(expected);

        OnboardingStatusResponse actual = onboardingService.getOnboardingStatus(1L);

        assertSame(expected, actual);
        verify(onboardingMapper).findOnboardingStatus(1L);
    }

    @Test
    void 회원이_없으면_NOT_FOUND_예외가_발생한다() {
        when(onboardingMapper.findOnboardingStatus(1L)).thenReturn(null);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> onboardingService.getOnboardingStatus(1L));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(onboardingMapper).findOnboardingStatus(1L);
    }
}
