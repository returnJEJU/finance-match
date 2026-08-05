package com.financematch.recommendation.mapper;

import com.financematch.recommendation.domain.JointRecommendationProduct;
import com.financematch.recommendation.domain.PersonalInvestmentProduct;
import com.financematch.recommendation.domain.PersonalTaxSavingProduct;
import com.financematch.recommendation.domain.Recommendation;
import com.financematch.recommendation.domain.RecommendationResult;
import com.financematch.recommendation.domain.RecommendationSlot;
import com.financematch.recommendation.policy.RecommendedProduct;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationMapper {

    Long findCoupleIdByMemberId(@Param("memberId") Long memberId);

    Long lockCoupleIdByIdNowait(@Param("coupleId") Long coupleId);

    LocalDateTime findLatestInputUpdatedAtByMemberId(
            @Param("memberId") Long memberId);

    Long findRecommendationIdByMemberId(
            @Param("memberId") Long memberId);

    RecommendationResult findRecommendationResultByMemberId(
            @Param("memberId") Long memberId);

    List<JointRecommendationProduct> findJointProductsByRecommendationIdAndMemberId(
            @Param("recommendationId") Long recommendationId,
            @Param("memberId") Long memberId);

    List<PersonalTaxSavingProduct> findPersonalTaxSavingByMemberId(
            @Param("memberId") Long memberId);

    List<PersonalInvestmentProduct> findPersonalInvestmentByMemberId(
            @Param("memberId") Long memberId);

    boolean isRecommendationCurrentByMemberId(
            @Param("memberId") Long memberId);

    int upsertRecommendation(Recommendation recommendation);

    int deleteSlotsByRecommendationId(
            @Param("recommendationId") Long recommendationId);

    int insertRecommendationSlot(RecommendationSlot slot);

    int insertRecommendationProduct(
            @Param("slotId") Long slotId,
            @Param("product") RecommendedProduct product);

    int deletePersonalTaxSavingByMemberId(
            @Param("memberId") Long memberId);

    int insertPersonalTaxSaving(
            @Param("memberId") Long memberId,
            @Param("product") RecommendedProduct product);

    int deletePersonalInvestmentByMemberId(
            @Param("memberId") Long memberId);

    int insertPersonalInvestment(
            @Param("memberId") Long memberId,
            @Param("product") RecommendedProduct product);
}