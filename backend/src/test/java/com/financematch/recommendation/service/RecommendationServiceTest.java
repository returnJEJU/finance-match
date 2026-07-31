package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.common.ErrorCode;
import com.financematch.config.RootConfig;
import com.financematch.exception.ApiException;
import com.financematch.recommendation.dto.PackageSlotResponse;
import com.financematch.recommendation.dto.RecommendationResponse;
import com.financematch.recommendation.mapper.RecommendationFreshnessTestMapper;
import com.financematch.recommendation.mapper.RecommendationMapper;
import com.financematch.recommendation.mapper.RecommendationTestMapper;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.type.PersonalRecommendationType;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@WebAppConfiguration
@Transactional
class RecommendationServiceTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RecommendationMapper recommendationMapper;

    @Autowired
    private RecommendationTestMapper recommendationTestMapper;

    @Autowired
    private RecommendationFreshnessTestMapper recommendationFreshnessTestMapper;

    @Test
    void 생성진입점으로_추천결과를_저장하고_조회한다() {

        Long memberId = 1L;

        recommendationService.createRecommendation(memberId);
        RecommendationResponse response =
                recommendationService.getRecommendation(memberId);

        assertNotNull(response);
        assertNotNull(response.recommendationId());
        assertFalse(response.packageSlots().isEmpty());
    }

    @Test
    void 커플중_한명의_금융정보가_없으면_추천을_생성하지_않는다() {

        Long memberId = 1L;
        assertEquals(
                1,
                recommendationFreshnessTestMapper
                        .deleteFinancialSummaryByMemberId(memberId));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.createRecommendation(memberId));

        assertEquals(ErrorCode.RECOMMENDATION_NOT_READY, exception.getErrorCode());
    }

    @Test
    void 회원ID로_공동_예금과_적금추천을_생성한다() {

        // given
        Long memberId = 1L;

        // when
        RecommendationPlan plan =
                recommendationService.recommend(memberId);

        // then
        assertNotNull(plan);

        List<RecommendedProduct> deposits =
                plan.joint()
                        .get(RecommendationSlotType.DEPOSIT);

        assertNotNull(deposits);
        assertEquals(2, deposits.size());

        RecommendedProduct first = deposits.get(0);
        RecommendedProduct second = deposits.get(1);

        assertEquals(2L, first.productId());
        assertEquals(1, first.rank());
        assertTrue(first.selected());

        assertEquals(1L, second.productId());
        assertEquals(2, second.rank());
        assertFalse(second.selected());

        // 적금
        List<RecommendedProduct> savings =
                plan.joint()
                        .get(RecommendationSlotType.SAVINGS);

        assertNotNull(savings);

        System.out.println("예금 추천 결과 = " + deposits);
        System.out.println("적금 추천 결과 = " + savings);
    }

    @Test
    void 추천결과를_DB에_저장하고_재실행해도_커플별_최신_1개만_유지한다() {

        // given
        Long memberId = 1L;
        Long coupleId = 1L;

        // when
        RecommendationPlan firstPlan =
                recommendationService.recommend(memberId);
        Long firstRecommendationId =
                recommendationMapper.findRecommendationIdByMemberId(memberId);

        RecommendationPlan secondPlan =
                recommendationService.recommend(memberId);
        Long secondRecommendationId =
                recommendationMapper.findRecommendationIdByMemberId(memberId);

        // then
        assertNotNull(firstRecommendationId);
        assertEquals(firstRecommendationId, secondRecommendationId);
        assertEquals(1, recommendationTestMapper.countRecommendationsByCoupleId(coupleId));
        assertEquals(
                secondRecommendationId,
                recommendationTestMapper.findRecommendationIdByCoupleIdAndMemberId(
                        coupleId,
                        memberId));

        assertJointRecommendationsSaved(
                secondRecommendationId,
                secondPlan);
        assertPersonalRecommendationsSaved(secondPlan);
        assertJointRecommendationsSaved(
                firstRecommendationId,
                firstPlan);
    }

    @Test
    void 저장한_추천결과를_상품상세정보와_함께_조회한다() {

        // given
        Long memberId = 1L;
        recommendationService.recommend(memberId);

        // when
        RecommendationResponse response =
                recommendationService.getRecommendation(memberId);

        // then
        assertNotNull(response);
        assertNotNull(response.recommendationId());
        assertFalse(response.packageSlots().isEmpty());

        PackageSlotResponse depositSlot =
                response.packageSlots().stream()
                        .filter(slot -> slot.slotType() == RecommendationSlotType.DEPOSIT)
                        .findFirst()
                        .orElseThrow();

        assertFalse(depositSlot.products().isEmpty());
        assertTrue(
                depositSlot.products().stream()
                        .anyMatch(
                                product ->
                                        product.productId()
                                                .equals(depositSlot.selectedProductId())));
        assertTrue(
                depositSlot.products().stream()
                        .allMatch(
                                product ->
                                        product.productUrl() != null
                                                && !product.productUrl().isBlank()));
        assertTrue(
                depositSlot.products().stream()
                        .allMatch(
                                product ->
                                        "기본금리".equals(product.comparisonLabel())
                                                && product.comparisonValue().startsWith("연 ")));
    }

    @Test
    void 커플_두회원은_같은_공동추천과_각자의_개인추천만_조회한다() {

        // given
        Long inviterId = 1L;
        Long inviteeId = 2L;
        recommendationService.recommend(inviterId);

        // when
        RecommendationResponse inviterResponse =
                recommendationService.getRecommendation(inviterId);
        RecommendationResponse inviteeResponse =
                recommendationService.getRecommendation(inviteeId);

        // then
        assertEquals(
                inviterResponse.recommendationId(),
                inviteeResponse.recommendationId());
        assertEquals(
                inviterResponse.packageSlots().size(),
                inviteeResponse.packageSlots().size());

        if (inviterResponse.personalTaxSavingRecommendation() != null) {
            assertEquals(
                    inviterId,
                    inviterResponse.personalTaxSavingRecommendation().targetMemberId());
        }
        if (inviteeResponse.personalTaxSavingRecommendation() != null) {
            assertEquals(
                    inviteeId,
                    inviteeResponse.personalTaxSavingRecommendation().targetMemberId());
        }
        if (inviterResponse.personalInvestmentRecommendation() != null) {
            assertEquals(
                    inviterId,
                    inviterResponse.personalInvestmentRecommendation().targetMemberId());
        }
        if (inviteeResponse.personalInvestmentRecommendation() != null) {
            assertEquals(
                    inviteeId,
                    inviteeResponse.personalInvestmentRecommendation().targetMemberId());
        }

        int personalInvestmentOwnerCount =
                (inviterResponse.personalInvestmentRecommendation() == null ? 0 : 1)
                        + (inviteeResponse.personalInvestmentRecommendation() == null ? 0 : 1);
        assertTrue(personalInvestmentOwnerCount <= 1);
    }

    @Test
    void 금융정보가_추천보다_최신이면_기존추천을_반환하지_않는다() {

        // given
        Long memberId = 1L;
        recommendationService.recommend(memberId);
        assertEquals(
                1,
                recommendationFreshnessTestMapper
                        .moveFinancialSummaryAfterRecommendation(memberId));

        // when
        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.getRecommendation(memberId));

        // then
        assertEquals(ErrorCode.RECOMMENDATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 로그인시각이_추천보다_최신이어도_기존추천을_반환한다() {

        // given
        Long memberId = 1L;
        recommendationService.recommend(memberId);
        assertEquals(
                1,
                recommendationFreshnessTestMapper
                        .moveLastLoginAfterRecommendation(memberId));

        // when
        RecommendationResponse response =
                recommendationService.getRecommendation(memberId);

        // then
        assertNotNull(response);
    }

    @Test
    void 투자성향이_추천보다_최신이면_기존추천을_반환하지_않는다() {

        // given
        Long memberId = 1L;
        recommendationService.recommend(memberId);
        assertEquals(
                1,
                recommendationFreshnessTestMapper
                        .moveInvestmentTypeAfterRecommendation(memberId));

        // when
        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.getRecommendation(memberId));

        // then
        assertEquals(ErrorCode.RECOMMENDATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 공동설문이_추천보다_최신이면_기존추천을_반환하지_않는다() {

        Long memberId = 1L;
        recommendationService.recommend(memberId);
        assertEquals(
                1,
                recommendationFreshnessTestMapper
                        .moveCommonSurveyAfterRecommendation(memberId));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.getRecommendation(memberId));

        assertEquals(ErrorCode.RECOMMENDATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 연금ISA정보가_추천보다_최신이면_기존추천을_반환하지_않는다() {

        Long memberId = 1L;
        recommendationService.recommend(memberId);
        assertEquals(
                1,
                recommendationFreshnessTestMapper
                        .movePensionIsaAccountAfterRecommendation(memberId));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.getRecommendation(memberId));

        assertEquals(ErrorCode.RECOMMENDATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 상품정보가_추천보다_최신이면_기존추천을_반환하지_않는다() {

        Long memberId = 1L;
        recommendationService.recommend(memberId);
        assertEquals(
                1,
                recommendationFreshnessTestMapper
                        .moveProductAfterRecommendation(memberId));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.getRecommendation(memberId));

        assertEquals(ErrorCode.RECOMMENDATION_NOT_FOUND, exception.getErrorCode());
    }

    private void assertJointRecommendationsSaved(
            Long recommendationId,
            RecommendationPlan plan) {

        int expectedSlotCount =
                (int)
                        plan.joint()
                                .values()
                                .stream()
                                .filter(products -> !products.isEmpty())
                                .count();

        int expectedProductCount =
                plan.joint()
                        .values()
                        .stream()
                        .mapToInt(List::size)
                        .sum();

        assertEquals(
                expectedSlotCount,
                recommendationTestMapper.countSlotsByRecommendationId(recommendationId));
        assertEquals(
                expectedProductCount,
                recommendationTestMapper.countProductsByRecommendationId(recommendationId));
        assertEquals(
                expectedSlotCount,
                recommendationTestMapper.countSelectedProductsByRecommendationId(recommendationId));

        plan.joint()
                .forEach(
                        (slotType, products) -> {
                            if (products.isEmpty()) {
                                return;
                            }
                            assertEquals(
                                    1,
                                    recommendationTestMapper.countSlotsByType(
                                            recommendationId,
                                            slotType));
                        });
    }

    private void assertPersonalRecommendationsSaved(RecommendationPlan plan) {
        Map<Long, List<RecommendedProduct>> taxSavingRecommendations =
                plan.personal()
                        .get(PersonalRecommendationType.TAX_SAVING);

        Map<Long, List<RecommendedProduct>> investmentRecommendations =
                plan.personal()
                        .get(PersonalRecommendationType.INVESTMENT);

        assertEquals(
                productCount(taxSavingRecommendations, 1L),
                recommendationTestMapper.countPersonalTaxSavingByMemberId(1L));
        assertEquals(
                productCount(taxSavingRecommendations, 2L),
                recommendationTestMapper.countPersonalTaxSavingByMemberId(2L));
        assertEquals(
                productCount(investmentRecommendations, 1L),
                recommendationTestMapper.countPersonalInvestmentByMemberId(1L));
        assertEquals(
                productCount(investmentRecommendations, 2L),
                recommendationTestMapper.countPersonalInvestmentByMemberId(2L));
    }

    private int productCount(
            Map<Long, List<RecommendedProduct>> recommendationsByMemberId,
            Long memberId) {

        if (recommendationsByMemberId == null
                || recommendationsByMemberId.get(memberId) == null) {
            return 0;
        }

        return recommendationsByMemberId.get(memberId).size();
    }
}
