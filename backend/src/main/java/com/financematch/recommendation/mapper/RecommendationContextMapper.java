package com.financematch.recommendation.mapper;

import com.financematch.recommendation.domain.RecommendationContext;
import org.apache.ibatis.annotations.Param;

public interface RecommendationContextMapper {

    RecommendationContext findByMemberId(@Param("memberId") Long memberId);
}
