package com.financematch.recommendation.service;

import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.mapper.RecommendationContextMapper;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private final RecommendationContextMapper recommendationContextMapper;
    private final RecommendationPlanner recommendationPlanner;

    public RecommendationService(
            RecommendationContextMapper recommendationContextMapper,
            RecommendationPlanner recommendationPlanner) {

        this.recommendationContextMapper = recommendationContextMapper;
        this.recommendationPlanner = recommendationPlanner;
    }

    public RecommendationPlan recommend(Long memberId) {

        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        RecommendationContext context =
                recommendationContextMapper.findByMemberId(memberId);

        if (context == null) {
            throw new IllegalStateException(
                    "추천에 필요한 부부 정보를 찾을 수 없습니다. memberId=" + memberId);
        }

        return recommendationPlanner.create(context);
    }
}