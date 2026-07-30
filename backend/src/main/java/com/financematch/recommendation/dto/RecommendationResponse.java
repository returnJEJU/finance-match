package com.financematch.recommendation.dto;

import java.util.List;

public record RecommendationResponse(
        Long recommendationId,
        boolean hasHighInterestDebt,
        List<PackageSlotResponse> packageSlots,
        PersonalTaxSavingRecommendationResponse personalTaxSavingRecommendation,
        PersonalInvestmentRecommendationResponse personalInvestmentRecommendation) {

    public RecommendationResponse {
        if (recommendationId == null) {
            throw new IllegalArgumentException("추천 결과 ID는 필수입니다.");
        }
        packageSlots = List.copyOf(packageSlots);
    }
}
