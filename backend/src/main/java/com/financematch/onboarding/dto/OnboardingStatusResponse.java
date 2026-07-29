package com.financematch.onboarding.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OnboardingStatusResponse {

    private boolean coupleConnected;
    private boolean hasInvitation;
    private boolean personalSurveyCompleted;
    private Boolean partnerPersonalSurveyCompleted;
}
