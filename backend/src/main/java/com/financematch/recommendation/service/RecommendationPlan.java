package com.financematch.recommendation.service;

import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.type.PersonalRecommendationType;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;
import java.util.Map;

public record RecommendationPlan(
        Map<RecommendationSlotType, List<RecommendedProduct>> joint,
        Map<PersonalRecommendationType, Map<Long, List<RecommendedProduct>>> personal) {

    public RecommendationPlan {
        joint = Map.copyOf(joint);
        personal = Map.copyOf(personal);
    }
}
