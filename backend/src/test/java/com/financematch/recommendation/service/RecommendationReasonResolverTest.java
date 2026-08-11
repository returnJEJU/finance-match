package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.financematch.recommendation.type.RecommendationReasonCode;
import org.junit.jupiter.api.Test;

class RecommendationReasonResolverTest {

    private final RecommendationReasonResolver resolver = new RecommendationReasonResolver();

    @Test
    void 모든_추천이유_코드를_문장으로_변환한다() {
        for (RecommendationReasonCode reasonCode : RecommendationReasonCode.values()) {
            String reason = resolver.resolve(reasonCode);

            assertNotNull(reason);
            assertFalse(reason.isBlank());
            int sentenceLength = reason.endsWith(".") ? reason.length() - 1 : reason.length();
            assertFalse(sentenceLength < 28);
            assertFalse(sentenceLength > 34);
        }
    }

    @Test
    void 추천이유_코드가_없으면_null을_반환한다() {
        assertNull(resolver.resolve(null));
    }
}