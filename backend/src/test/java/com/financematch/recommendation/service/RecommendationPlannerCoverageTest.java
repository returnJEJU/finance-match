package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.PersonalRecommendationPolicy;
import com.financematch.recommendation.policy.joint.JointRecommendationPolicy;
import com.financematch.recommendation.type.PersonalRecommendationType;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecommendationPlannerCoverageTest {

    @Test
    void rejectsDuplicateJointPolicyType() {
        JointRecommendationPolicy first = jointPolicy(RecommendationSlotType.DEPOSIT);
        JointRecommendationPolicy second = jointPolicy(RecommendationSlotType.DEPOSIT);
        RecommendationPlanner planner = new RecommendationPlanner(List.of(first, second), List.of());

        assertThrows(IllegalStateException.class, () -> planner.create(new RecommendationContext()));
    }

    @Test
    void rejectsDuplicatePersonalPolicyType() {
        PersonalRecommendationPolicy first = personalPolicy(PersonalRecommendationType.TAX_SAVING);
        PersonalRecommendationPolicy second = personalPolicy(PersonalRecommendationType.TAX_SAVING);
        RecommendationPlanner planner = new RecommendationPlanner(List.of(), List.of(first, second));

        assertThrows(IllegalStateException.class, () -> planner.create(new RecommendationContext()));
    }

    private JointRecommendationPolicy jointPolicy(RecommendationSlotType type) {
        return new JointRecommendationPolicy() {
            @Override
            public RecommendationSlotType slotType() {
                return type;
            }

            @Override
            public List<com.financematch.recommendation.policy.RecommendedProduct> recommend(
                    RecommendationContext context) {
                return List.of();
            }
        };
    }

    private PersonalRecommendationPolicy personalPolicy(PersonalRecommendationType type) {
        return new PersonalRecommendationPolicy() {
            @Override
            public PersonalRecommendationType recommendationType() {
                return type;
            }

            @Override
            public Map<Long, List<com.financematch.recommendation.policy.RecommendedProduct>> recommend(
                    RecommendationContext context) {
                return Map.of();
            }
        };
    }
}
