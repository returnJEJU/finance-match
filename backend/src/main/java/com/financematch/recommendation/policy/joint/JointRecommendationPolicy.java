package com.financematch.recommendation.policy.joint;

import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;

public interface JointRecommendationPolicy {

    RecommendationSlotType slotType();

    List<RecommendedProduct> recommend(RecommendationContext context);
}
