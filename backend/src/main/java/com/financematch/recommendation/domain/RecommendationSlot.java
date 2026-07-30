package com.financematch.recommendation.domain;

import com.financematch.recommendation.type.RecommendationSlotType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecommendationSlot {

    // recommendation_slot.id: 슬롯 상품 후보를 묶는 부모 row ID
    private Long id;

    // recommendation_slot.recommendation_id: recommendation 부모 row ID
    private Long recommendationId;

    // recommendation_slot.slot_type: DEPOSIT, SAVINGS, INVESTMENT, LOAN
    private RecommendationSlotType slotType;

    public RecommendationSlot(Long recommendationId, RecommendationSlotType slotType) {
        this.recommendationId = recommendationId;
        this.slotType = slotType;
    }
}
