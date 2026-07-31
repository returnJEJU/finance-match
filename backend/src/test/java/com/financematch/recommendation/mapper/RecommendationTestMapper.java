package com.financematch.recommendation.mapper;

import com.financematch.recommendation.type.RecommendationSlotType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationTestMapper {

    Long findRecommendationIdByCoupleIdAndMemberId(
            @Param("coupleId") Long coupleId,
            @Param("memberId") Long memberId);

    int countRecommendationsByCoupleId(@Param("coupleId") Long coupleId);

    int countSlotsByRecommendationId(@Param("recommendationId") Long recommendationId);

    int countProductsByRecommendationId(@Param("recommendationId") Long recommendationId);

    int countSelectedProductsByRecommendationId(@Param("recommendationId") Long recommendationId);

    int countSlotsByType(
            @Param("recommendationId") Long recommendationId,
            @Param("slotType") RecommendationSlotType slotType);

    int countPersonalTaxSavingByMemberId(@Param("memberId") Long memberId);

    int countPersonalInvestmentByMemberId(@Param("memberId") Long memberId);
}