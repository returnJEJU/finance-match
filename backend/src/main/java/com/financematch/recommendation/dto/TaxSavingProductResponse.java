package com.financematch.recommendation.dto;

import com.financematch.product.type.TaxAccountType;

public record TaxSavingProductResponse(
        Long productId,
        String productName,
        String description,
        String productUrl,
        TaxAccountType accountType) {

    public TaxSavingProductResponse {
        if (productId == null || accountType == null) {
            throw new IllegalArgumentException("절세 추천 상품의 필수값이 누락되었습니다.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("절세 추천 상품명은 필수입니다.");
        }
        if (productUrl == null || productUrl.isBlank()) {
            throw new IllegalArgumentException("절세 추천 상품 공식 URL은 필수입니다.");
        }
    }
}
