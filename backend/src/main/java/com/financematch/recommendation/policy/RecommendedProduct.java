package com.financematch.recommendation.policy;

import com.financematch.recommendation.type.RecommendationReasonCode;

public record RecommendedProduct(
        Long productId,
        int rank,
        boolean selected,
        RecommendationReasonCode reasonCode,
        String recommendationReason) {

    public RecommendedProduct(Long productId, int rank, boolean selected) {
        this(productId, rank, selected, null, null);
    }

    public RecommendedProduct(
            Long productId,
            int rank,
            boolean selected,
            RecommendationReasonCode reasonCode) {
        this(productId, rank, selected, reasonCode, null);
    }

    public RecommendedProduct {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }
        if (rank < 1) {
            throw new IllegalArgumentException("추천 순위는 1 이상이어야 합니다.");
        }
        if (!selected && (reasonCode != null || recommendationReason != null)) {
            throw new IllegalArgumentException("대안 상품에는 추천 이유를 지정할 수 없습니다.");
        }
        if (recommendationReason != null && reasonCode == null) {
            throw new IllegalArgumentException("추천 이유 문장에는 근거 코드가 필요합니다.");
        }
    }

    public RecommendedProduct withRecommendationReason(String reason) {
        return new RecommendedProduct(productId, rank, selected, reasonCode, reason);
    }
}