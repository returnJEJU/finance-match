package com.financematch.recommendation.service;

import com.financematch.recommendation.domain.Recommendation;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.domain.RecommendationSlot;
import com.financematch.recommendation.mapper.RecommendationContextMapper;
import com.financematch.recommendation.mapper.RecommendationMapper;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.type.PersonalRecommendationType;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private final RecommendationContextMapper recommendationContextMapper;
    private final RecommendationMapper recommendationMapper;
    private final RecommendationPlanner recommendationPlanner;

    public RecommendationService(
            RecommendationContextMapper recommendationContextMapper,
            RecommendationMapper recommendationMapper,
            RecommendationPlanner recommendationPlanner) {

        this.recommendationContextMapper = recommendationContextMapper;
        this.recommendationMapper = recommendationMapper;
        this.recommendationPlanner = recommendationPlanner;
    }

    @Transactional
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

        RecommendationPlan plan = recommendationPlanner.create(context);
        saveRecommendationResult(context, plan);

        return plan;
    }

    private void saveRecommendationResult(
            RecommendationContext context,
            RecommendationPlan plan) {

        Long recommendationId = getOrCreateRecommendationId(context.getCoupleId());

        // 공동 추천은 recommendation 하위 슬롯과 후보 상품을 통째로 최신 결과로 교체한다.
        replaceJointRecommendations(
                recommendationId,
                plan.joint());

        // 개인 추천은 회원별 최신 절세·투자 결과만 남도록 기존 row를 지우고 다시 넣는다.
        replacePersonalRecommendations(
                context.memberIds(),
                plan.personal());
    }

    private Long getOrCreateRecommendationId(Long coupleId) {
        if (coupleId == null) {
            throw new IllegalStateException("추천 결과를 저장할 커플 ID가 필요합니다.");
        }

        Recommendation recommendation = new Recommendation(coupleId);
        recommendationMapper.upsertRecommendation(recommendation);

        if (recommendation.getId() == null) {
            throw new IllegalStateException("추천 결과 부모 row 저장에 실패했습니다.");
        }

        return recommendation.getId();
    }

    private void replaceJointRecommendations(
            Long recommendationId,
            Map<RecommendationSlotType, List<RecommendedProduct>> jointRecommendations) {

        recommendationMapper.deleteSlotsByRecommendationId(recommendationId);

        jointRecommendations.forEach(
                (slotType, products) -> {
                    if (products == null || products.isEmpty()) {
                        return;
                    }

                    RecommendationSlot slot =
                            new RecommendationSlot(
                                    recommendationId,
                                    slotType);
                    recommendationMapper.insertRecommendationSlot(slot);

                    if (slot.getId() == null) {
                        throw new IllegalStateException("추천 슬롯 저장에 실패했습니다: " + slotType);
                    }

                    products.forEach(
                            product ->
                                    recommendationMapper.insertRecommendationProduct(
                                            slot.getId(),
                                            product));
                });
    }

    private void replacePersonalRecommendations(
            List<Long> memberIds,
            Map<PersonalRecommendationType, Map<Long, List<RecommendedProduct>>> personalRecommendations) {

        memberIds.forEach(
                memberId -> {
                    recommendationMapper.deletePersonalTaxSavingByMemberId(memberId);
                    recommendationMapper.deletePersonalInvestmentByMemberId(memberId);
                });

        savePersonalTaxSavingRecommendations(
                personalRecommendations.get(PersonalRecommendationType.TAX_SAVING));

        savePersonalInvestmentRecommendations(
                personalRecommendations.get(PersonalRecommendationType.INVESTMENT));
    }

    private void savePersonalTaxSavingRecommendations(
            Map<Long, List<RecommendedProduct>> recommendationsByMemberId) {

        if (recommendationsByMemberId == null) {
            return;
        }

        recommendationsByMemberId.forEach(
                (memberId, products) ->
                        products.forEach(
                                product ->
                                        recommendationMapper.insertPersonalTaxSaving(
                                                memberId,
                                                product)));
    }

    private void savePersonalInvestmentRecommendations(
            Map<Long, List<RecommendedProduct>> recommendationsByMemberId) {

        if (recommendationsByMemberId == null) {
            return;
        }

        recommendationsByMemberId.forEach(
                (memberId, products) ->
                        products.forEach(
                                product ->
                                        recommendationMapper.insertPersonalInvestment(
                                                memberId,
                                                product)));
    }
}
