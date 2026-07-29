package com.financematch.recommendation.policy;

public record RecommendedProduct(Long productId, int rank, boolean selected) {

    public RecommendedProduct {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }
        if (rank < 1) {
            throw new IllegalArgumentException("추천 순위는 1 이상이어야 합니다.");
        }
    }
}
