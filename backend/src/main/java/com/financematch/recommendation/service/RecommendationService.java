package com.financematch.recommendation.service;

import com.financematch.common.ErrorCode;
import com.financematch.couple.mapper.CoupleMapper;
import com.financematch.exception.ApiException;
import com.financematch.recommendation.domain.Recommendation;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.domain.RecommendationResult;
import com.financematch.recommendation.domain.RecommendationSlot;
import com.financematch.recommendation.dto.RecommendationResponse;
import com.financematch.recommendation.mapper.RecommendationContextMapper;
import com.financematch.recommendation.mapper.RecommendationMapper;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.type.PersonalRecommendationType;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RecommendationService {

    private final RecommendationContextMapper recommendationContextMapper;
    private final RecommendationMapper recommendationMapper;
    private final RecommendationPlanner recommendationPlanner;
    private final CoupleMapper coupleMapper;
    private final RecommendationResponseAssembler responseAssembler;

    public RecommendationService(
            RecommendationContextMapper recommendationContextMapper,
            RecommendationMapper recommendationMapper,
            RecommendationPlanner recommendationPlanner,
            CoupleMapper coupleMapper,
            RecommendationResponseAssembler responseAssembler) {

        this.recommendationContextMapper = recommendationContextMapper;
        this.recommendationMapper = recommendationMapper;
        this.recommendationPlanner = recommendationPlanner;
        this.coupleMapper = coupleMapper;
        this.responseAssembler = responseAssembler;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createRecommendation(Long memberId) {
        try {
            Long coupleId =
                    recommendationMapper.findCoupleIdByMemberId(memberId);

            if (coupleId == null) {
                throw new ApiException(ErrorCode.COUPLE_NOT_CONNECTED);
            }

            Long lockedCoupleId =
                    recommendationMapper.lockCoupleIdByIdNowait(coupleId);

            if (lockedCoupleId == null) {
                throw new ApiException(ErrorCode.COUPLE_NOT_CONNECTED);
            }

            LocalDateTime inputVersionBefore =
                    recommendationMapper.findLatestInputUpdatedAtByMemberId(memberId);

            recommend(memberId);

            LocalDateTime inputVersionAfter =
                    recommendationMapper.findLatestInputUpdatedAtByMemberId(memberId);

            if (!Objects.equals(inputVersionBefore, inputVersionAfter)) {
                throw new RecommendationInputChangedException(
                        inputVersionBefore,
                        inputVersionAfter);
            }
        } catch (CannotAcquireLockException exception) {
            log.warn("추천 생성 중복 요청. memberId={}", memberId);
            throw new ApiException(ErrorCode.RECOMMENDATION_IN_PROGRESS);
        } catch (ApiException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            log.warn("추천 생성 준비 정보 부족. memberId={}", memberId, exception);
            throw new ApiException(ErrorCode.RECOMMENDATION_NOT_READY);
        } catch (Exception exception) {
            log.error("추천 생성 실패. memberId={}", memberId, exception);
            throw new ApiException(ErrorCode.RECOMMENDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendation(Long memberId) {
        try {
            RecommendationResult result =
                    recommendationMapper.findRecommendationResultByMemberId(memberId);

            if (result == null) {
                if (!coupleMapper.existsCoupleByMemberId(memberId)) {
                    throw new ApiException(ErrorCode.COUPLE_NOT_CONNECTED);
                }

                throw new ApiException(ErrorCode.RECOMMENDATION_NOT_FOUND);
            }

            if (!recommendationMapper.isRecommendationCurrentByMemberId(memberId)) {
                throw new ApiException(ErrorCode.RECOMMENDATION_NOT_FOUND);
            }

            return responseAssembler.assemble(
                    result,
                    recommendationMapper.findJointProductsByRecommendationIdAndMemberId(
                            result.getRecommendationId(),
                            memberId),
                    recommendationMapper.findPersonalTaxSavingByMemberId(memberId),
                    recommendationMapper.findPersonalInvestmentByMemberId(memberId));
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("추천 조회 실패. memberId={}", memberId, exception);
            throw new ApiException(ErrorCode.RECOMMENDATION_FAILED);
        }
    }

    RecommendationPlan recommend(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        RecommendationContext context =
                recommendationContextMapper.findByMemberId(memberId);

        if (context == null) {
            if (!coupleMapper.existsCoupleByMemberId(memberId)) {
                throw new ApiException(ErrorCode.COUPLE_NOT_CONNECTED);
            }

            throw new ApiException(ErrorCode.RECOMMENDATION_NOT_READY);
        }

        RecommendationPlan plan =
                recommendationPlanner.create(context);

        saveRecommendationResult(context, plan);

        return plan;
    }

    private void saveRecommendationResult(
            RecommendationContext context,
            RecommendationPlan plan) {

        Long recommendationId =
                getOrCreateRecommendationId(context.getCoupleId());

        replaceJointRecommendations(
                recommendationId,
                plan.joint());

        replacePersonalRecommendations(
                context.memberIds(),
                plan.personal());
    }

    private Long getOrCreateRecommendationId(Long coupleId) {
        if (coupleId == null) {
            throw new IllegalStateException(
                    "추천 결과를 저장할 커플 ID가 필요합니다.");
        }

        Recommendation recommendation =
                new Recommendation(coupleId);

        recommendationMapper.upsertRecommendation(recommendation);

        if (recommendation.getId() == null) {
            throw new IllegalStateException(
                    "추천 결과 부모 row 저장에 실패했습니다.");
        }

        return recommendation.getId();
    }

    private void replaceJointRecommendations(
            Long recommendationId,
            Map<RecommendationSlotType, List<RecommendedProduct>>
                    jointRecommendations) {

        recommendationMapper.deleteSlotsByRecommendationId(
                recommendationId);

        jointRecommendations.forEach(
                (slotType, products) -> {
                    if (products.isEmpty()) {
                        return;
                    }

                    RecommendationSlot slot =
                            new RecommendationSlot(
                                    recommendationId,
                                    slotType);

                    recommendationMapper.insertRecommendationSlot(slot);

                    if (slot.getId() == null) {
                        throw new IllegalStateException(
                                "추천 슬롯 저장에 실패했습니다: " + slotType);
                    }

                    products.forEach(
                            product ->
                                    recommendationMapper
                                            .insertRecommendationProduct(
                                                    slot.getId(),
                                                    product));
                });
    }

    private void replacePersonalRecommendations(
            List<Long> memberIds,
            Map<
                    PersonalRecommendationType,
                    Map<Long, List<RecommendedProduct>>>
                    personalRecommendations) {

        memberIds.forEach(
                memberId -> {
                    recommendationMapper
                            .deletePersonalTaxSavingByMemberId(memberId);

                    recommendationMapper
                            .deletePersonalInvestmentByMemberId(memberId);
                });

        savePersonalTaxSavingRecommendations(
                personalRecommendations.get(
                        PersonalRecommendationType.TAX_SAVING));

        savePersonalInvestmentRecommendations(
                personalRecommendations.get(
                        PersonalRecommendationType.INVESTMENT));
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
                                        recommendationMapper
                                                .insertPersonalTaxSaving(
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
                                        recommendationMapper
                                                .insertPersonalInvestment(
                                                        memberId,
                                                        product)));
    }

    private static class RecommendationInputChangedException
            extends RuntimeException {

        private RecommendationInputChangedException(
                LocalDateTime before,
                LocalDateTime after) {

            super(
                    "추천 생성 중 입력정보가 변경되었습니다. before="
                            + before
                            + ", after="
                            + after);
        }
    }
}