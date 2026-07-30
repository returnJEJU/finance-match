package com.financematch.recommendation.dto;

public record PackageProductResponse(
        Long productId,
        String productName,
        String comparisonLabel,
        String comparisonValue) {

    public PackageProductResponse {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (comparisonLabel == null || comparisonLabel.isBlank()) {
            throw new IllegalArgumentException("상품 비교 기준명은 필수입니다.");
        }
        if (comparisonValue == null || comparisonValue.isBlank()) {
            throw new IllegalArgumentException("상품 비교값은 필수입니다.");
        }
    }
}
