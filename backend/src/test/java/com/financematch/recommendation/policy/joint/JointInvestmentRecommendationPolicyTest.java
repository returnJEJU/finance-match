package com.financematch.recommendation.policy.joint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.financematch.product.dto.InvestmentProduct;
import com.financematch.product.mapper.InvestmentMapper;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.policy.joint.JointInvestmentRecommendationPolicy;
import com.financematch.recommendation.service.InvestmentRiskLevelCalculator;
import com.financematch.recommendation.type.RecommendationReasonCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JointInvestmentRecommendationPolicyTest {

    @Mock private InvestmentMapper investmentMapper;

    @Test
    void usesMoreConservativeMembersRiskLevel() {
        when(investmentMapper.findAll())
                .thenReturn(
                        List.of(
                                product(1L, 1, 500),
                                product(2L, 5, 100),
                                product(3L, 5, 300)));
        JointInvestmentRecommendationPolicy policy =
                new JointInvestmentRecommendationPolicy(
                        investmentMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = new RecommendationContext();
        context.setInviterInvestmentType("VERY_AGGRESSIVE");
        context.setInviteeInvestmentType("STABLE_SEEKING");

        List<RecommendedProduct> result = policy.recommend(context);

        assertEquals(List.of(3L, 2L), productIds(result));
        assertTrue(result.get(0).selected());
        assertEquals(
                RecommendationReasonCode.INVESTMENT_RISK_AND_AUM,
                result.get(0).reasonCode());
    }

    @Test
    void returnsEmptyWhenCouplePlansLoanWithinOneMonth() {
        JointInvestmentRecommendationPolicy policy =
                new JointInvestmentRecommendationPolicy(
                        investmentMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = new RecommendationContext();
        context.setHasLoanWithinOneMonth(true);

        assertTrue(policy.recommend(context).isEmpty());
        verifyNoInteractions(investmentMapper);
    }

    @Test
    void prioritizesTdfWhenRetirementIsFirstGoal() {
        when(investmentMapper.findAll())
                .thenReturn(
                        List.of(
                                product(1L, 5, 500, false),
                                product(2L, 5, 300, true),
                                product(3L, 6, 400, true)));
        JointInvestmentRecommendationPolicy policy =
                new JointInvestmentRecommendationPolicy(
                        investmentMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = new RecommendationContext();
        context.setInviterInvestmentType("STABLE_SEEKING");
        context.setInviteeInvestmentType("VERY_AGGRESSIVE");
        context.setFirstGoalType("RETIREMENT");

        List<RecommendedProduct> result = policy.recommend(context);

        assertEquals(List.of(2L, 3L, 1L), productIds(result));
        assertEquals(
                RecommendationReasonCode.INVESTMENT_RETIREMENT_TDF_PRIORITY,
                result.get(0).reasonCode());
    }

    private InvestmentProduct product(Long productId, Integer riskLevel, Integer aum) {
        return product(productId, riskLevel, aum, false);
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
