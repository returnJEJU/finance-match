package com.financematch.recommendation.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.recommendation.type.RecommendationReasonCode;
import org.junit.jupiter.api.Test;

class RecommendedProductTest {

    @Test
    void supportsAllConstructorsAndReasonResolution() {
        RecommendedProduct plain = new RecommendedProduct(1L, 1, true);
        RecommendedProduct coded =
                new RecommendedProduct(
                        2L,
                        1,
                        true,
                        RecommendationReasonCode.LOAN_TARGET_GROUP_RATE_AND_CHANNEL);
        RecommendedProduct resolved = coded.withRecommendationReason("추천 이유");

        assertNull(plain.reasonCode());
        assertEquals("추천 이유", resolved.recommendationReason());
    }

    @Test
    void rejectsInvalidRecommendationProducts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecommendedProduct(null, 1, true));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecommendedProduct(1L, 0, true));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new RecommendedProduct(
                                1L,
                                1,
                                false,
                                RecommendationReasonCode.LOAN_TARGET_GROUP_RATE_AND_CHANNEL));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecommendedProduct(1L, 1, false, null, "추천 이유"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecommendedProduct(1L, 1, true, null, "추천 이유"));
    }
}
