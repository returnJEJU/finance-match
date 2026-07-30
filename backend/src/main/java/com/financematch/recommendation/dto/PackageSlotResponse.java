package com.financematch.recommendation.dto;

import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;

public record PackageSlotResponse(
        Long slotId,
        Long selectedProductId,
        RecommendationSlotType slotType,
        String slotName,
        List<PackageProductResponse> products) {

    public PackageSlotResponse {
        if (slotId == null || selectedProductId == null || slotType == null) {
            throw new IllegalArgumentException("추천 슬롯의 필수값이 누락되었습니다.");
        }
        if (slotName == null || slotName.isBlank()) {
            throw new IllegalArgumentException("추천 슬롯 이름은 필수입니다.");
        }
        products = List.copyOf(products);
        if (products.isEmpty()) {
            throw new IllegalArgumentException("활성화된 추천 슬롯에는 상품이 필요합니다.");
        }
        boolean containsSelectedProduct =
                products.stream()
                        .anyMatch(product -> product.productId().equals(selectedProductId));
        if (!containsSelectedProduct) {
            throw new IllegalArgumentException("선택 상품은 추천 슬롯의 상품 목록에 포함되어야 합니다.");
        }
    }
}
