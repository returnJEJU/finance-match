package com.financematch.recommendation.dto;

public record PersonalInvestmentProductResponse(
        Long productId,
        String productName,
        String investmentType) {

    public PersonalInvestmentProductResponse {
        if (productId == null) {
            throw new IllegalArgumentException("개인 투자 추천 상품 ID는 필수입니다.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("개인 투자 추천 상품명은 필수입니다.");
        }
        if (investmentType == null || investmentType.isBlank()) {
            throw new IllegalArgumentException("개인 투자성향은 필수입니다.");
        }
    }
}
