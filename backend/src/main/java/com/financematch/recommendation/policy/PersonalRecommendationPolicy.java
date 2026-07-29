package com.financematch.recommendation.policy;

import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.type.PersonalRecommendationType;
import java.util.List;
import java.util.Map;

public interface PersonalRecommendationPolicy {

    PersonalRecommendationType recommendationType();

    Map<Long, List<RecommendedProduct>> recommend(RecommendationContext context);
}
