package com.financematch.recommendation.dto;

import java.util.List;

public record PersonalInvestmentRecommendationResponse(
        Long targetMemberId,
        String targetMemberName,
        List<PersonalInvestmentProductResponse> products) {

    public PersonalInvestmentRecommendationResponse {
        if (targetMemberId == null) {
            throw new IllegalArgumentException("개인 투자 추천 대상 회원 ID는 필수입니다.");
        }
        if (targetMemberName == null || targetMemberName.isBlank()) {
            throw new IllegalArgumentException("개인 투자 추천 대상 회원 이름은 필수입니다.");
        }
        products = List.copyOf(products);
        if (products.isEmpty()) {
            throw new IllegalArgumentException("개인 투자 추천 대상이 없으면 응답 객체를 null로 반환해야 합니다.");
        }
    }
}
