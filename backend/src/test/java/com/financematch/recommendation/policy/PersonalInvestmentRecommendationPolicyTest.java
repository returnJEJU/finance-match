package com.financematch.recommendation.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.financematch.product.dto.InvestmentProduct;
import com.financematch.product.mapper.InvestmentMapper;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.service.InvestmentRiskLevelCalculator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonalInvestmentRecommendationPolicyTest {

    @Mock private InvestmentMapper investmentMapper;

    @Test
    void recommendsProductsOnlyForMoreAggressiveMember() {
        when(investmentMapper.findAll())
                .thenReturn(
                        List.of(
                                product(1L, 6, 100, false),
                                product(2L, 1, 100, false),
                                product(3L, 2, 200, false),
                                product(4L, 2, 300, true)));
        PersonalInvestmentRecommendationPolicy policy =
                new PersonalInvestmentRecommendationPolicy(
                        investmentMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = new RecommendationContext();
        context.setInviterId(10L);
        context.setInviteeId(20L);
        context.setInviterInvestmentType("STABLE");
        context.setInviteeInvestmentType("VERY_AGGRESSIVE");

        var result = policy.recommend(context);

        assertFalse(result.containsKey(10L));
        assertEquals(List.of(2L, 4L, 3L, 1L), productIds(result.get(20L)));
        assertTrue(result.get(20L).get(0).selected());
        assertFalse(result.get(20L).get(1).selected());
    }

    @Test
    void returnsEmptyMapWhenInvestmentTypesAreSame() {
        PersonalInvestmentRecommendationPolicy policy =
                new PersonalInvestmentRecommendationPolicy(
                        investmentMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = new RecommendationContext();
        context.setInviterId(10L);
        context.setInviteeId(20L);
        context.setInviterInvestmentType("STABLE");
        context.setInviteeInvestmentType("STABLE");

        var result = policy.recommend(context);

        assertTrue(result.isEmpty());
        verifyNoInteractions(investmentMapper);
    }

    @Test
    void returnsEmptyMapWhenCouplePlansLoanWithinOneMonth() {
        PersonalInvestmentRecommendationPolicy policy =
                new PersonalInvestmentRecommendationPolicy(
                        investmentMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = new RecommendationContext();
        context.setHasLoanWithinOneMonth(true);

        var result = policy.recommend(context);

        assertTrue(result.isEmpty());
        verifyNoInteractions(investmentMapper);
    }

    @Test
    void prioritizesTdfForRetirementGoal() {
        when(investmentMapper.findAll())
                .thenReturn(
                        List.of(
                                product(1L, 5, 500, false),
                                product(2L, 5, 300, true),
                                product(3L, 6, 400, true)));
        PersonalInvestmentRecommendationPolicy policy =
                new PersonalInvestmentRecommendationPolicy(
                        investmentMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = new RecommendationContext();
        context.setInviterId(10L);
        context.setInviteeId(20L);
        context.setInviterInvestmentType("STABLE_SEEKING");
        context.setInviteeInvestmentType("STABLE");
        context.setFirstGoalType("RETIREMENT");

        var result = policy.recommend(context);

        assertEquals(List.of(2L, 3L, 1L), productIds(result.get(10L)));
        assertFalse(result.containsKey(20L));
    }

    private InvestmentProduct product(
            Long productId, Integer riskLevel, Integer aum, boolean isTdf) {
        InvestmentProduct product = new InvestmentProduct();
        product.setProductId(productId);
        product.setRiskLevel(riskLevel);
        product.setAum(aum);
        product.setIsTdf(isTdf);
        return product;
    }

    private List<Long> productIds(List<RecommendedProduct> products) {
        return products.stream().map(RecommendedProduct::productId).toList();
    }
}
