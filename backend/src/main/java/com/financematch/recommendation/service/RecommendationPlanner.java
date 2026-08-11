package com.financematch.recommendation.service;

import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.PersonalRecommendationPolicy;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.policy.joint.JointRecommendationPolicy;
import com.financematch.recommendation.type.PersonalRecommendationType;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecommendationPlanner {

    private final List<JointRecommendationPolicy> jointPolicies;
    private final List<PersonalRecommendationPolicy> personalPolicies;
    private final RecommendationReasonResolver reasonResolver;

    @Autowired
    public RecommendationPlanner(
            List<JointRecommendationPolicy> jointPolicies,
            List<PersonalRecommendationPolicy> personalPolicies,
            RecommendationReasonResolver reasonResolver) {
        this.jointPolicies = List.copyOf(jointPolicies);
        this.personalPolicies = List.copyOf(personalPolicies);
        this.reasonResolver = reasonResolver;
    }

    public RecommendationPlanner(
            List<JointRecommendationPolicy> jointPolicies,
            List<PersonalRecommendationPolicy> personalPolicies) {
        this(jointPolicies, personalPolicies, new RecommendationReasonResolver());
    }

    public RecommendationPlan create(RecommendationContext context) {
        Map<RecommendationSlotType, List<RecommendedProduct>> joint =
                new EnumMap<>(RecommendationSlotType.class);
        for (JointRecommendationPolicy policy : jointPolicies) {
            List<RecommendedProduct> previous =
                    joint.put(
                            policy.slotType(),
                            resolveReasons(policy.recommend(context)));
            if (previous != null) {
                throw new IllegalStateException("공동 추천 정책 유형이 중복되었습니다: " + policy.slotType());
            }
        }

        Map<PersonalRecommendationType, Map<Long, List<RecommendedProduct>>> personal =
                new EnumMap<>(PersonalRecommendationType.class);
        for (PersonalRecommendationPolicy policy : personalPolicies) {
            Map<Long, List<RecommendedProduct>> result = copyPersonalResult(policy.recommend(context));
            Map<Long, List<RecommendedProduct>> previous =
                    personal.put(policy.recommendationType(), result);
            if (previous != null) {
                throw new IllegalStateException(
                        "개인 추천 정책 유형이 중복되었습니다: " + policy.recommendationType());
            }
        }
        return new RecommendationPlan(joint, personal);
    }

    private List<RecommendedProduct> resolveReasons(List<RecommendedProduct> products) {
        return products.stream()
                .map(
                        product ->
                                product.withRecommendationReason(
                                        reasonResolver.resolve(product.reasonCode())))
                .toList();
    }

    private Map<Long, List<RecommendedProduct>> copyPersonalResult(
            Map<Long, List<RecommendedProduct>> result) {
        Map<Long, List<RecommendedProduct>> copied = new java.util.LinkedHashMap<>();
        result.forEach((memberId, products) -> copied.put(memberId, resolveReasons(products)));
        return Map.copyOf(copied);
    }
}