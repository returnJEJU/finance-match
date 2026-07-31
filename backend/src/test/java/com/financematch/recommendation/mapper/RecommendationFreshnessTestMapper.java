package com.financematch.recommendation.mapper;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationFreshnessTestMapper {

    int moveFinancialSummaryAfterRecommendation(
            @Param("memberId") Long memberId);

    int moveLastLoginAfterRecommendation(
            @Param("memberId") Long memberId);

    int moveCommonSurveyAfterRecommendation(
            @Param("memberId") Long memberId);

    int movePensionIsaAccountAfterRecommendation(
            @Param("memberId") Long memberId);

    int moveProductAfterRecommendation(
            @Param("memberId") Long memberId);

    LocalDateTime findFinancialSummaryUpdatedAt(
            @Param("memberId") Long memberId);

    int moveFinancialSummaryToFuture(
            @Param("memberId") Long memberId);

    int restoreFinancialSummaryUpdatedAt(
            @Param("memberId") Long memberId,
            @Param("updatedAt") LocalDateTime updatedAt);

    int deleteFinancialSummaryByMemberId(
            @Param("memberId") Long memberId);
}
