package com.financematch.recommendation.dto;

public record PersonalInvestmentProductResponse(
        Long productId,
        String productName,
        String description,
        String productUrl,
        int riskLevel,
        String riskLabel,
        int aum) {

    public PersonalInvestmentProductResponse {
        if (productId == null) {
            throw new IllegalArgumentException("개인 투자 추천 상품 ID는 필수입니다.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("개인 투자 추천 상품명은 필수입니다.");
        }
        if (productUrl == null || productUrl.isBlank()) {
            throw new IllegalArgumentException("개인 투자 추천 상품 공식 URL은 필수입니다.");
        }
        if (riskLabel == null || riskLabel.isBlank()) {
            throw new IllegalArgumentException("개인 투자 추천 상품 위험등급 명칭은 필수입니다.");
        }
        if (aum < 0) {
            throw new IllegalArgumentException("개인 투자 추천 상품 순자산은 0 이상이어야 합니다.");
        }

        String expectedLabel =
                switch (riskLevel) {
                    case 1, 2 -> "초고위험";
                    case 3 -> "고위험";
                    case 4 -> "위험";
                    case 5 -> "중립";
                    case 6 -> "안정";
                    default ->
                            throw new IllegalArgumentException(
                                    "지원하지 않는 상품 위험등급입니다: " + riskLevel);
                };
        if (!expectedLabel.equals(riskLabel)) {
            throw new IllegalArgumentException("개인 투자 상품 위험등급과 명칭이 일치하지 않습니다.");
        }
    }
}
