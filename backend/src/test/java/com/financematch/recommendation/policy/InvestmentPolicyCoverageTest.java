package com.financematch.recommendation.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.product.dto.InvestmentProduct;
import com.financematch.product.mapper.InvestmentMapper;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.joint.JointInvestmentRecommendationPolicy;
import com.financematch.recommendation.service.InvestmentRiskLevelCalculator;
import com.financematch.recommendation.type.RecommendationReasonCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentPolicyCoverageTest {

    @Mock private InvestmentMapper investmentMapper;

    private final InvestmentRiskLevelCalculator calculator = new InvestmentRiskLevelCalculator();

    @Test
    void rejectsBlankInvestmentType() {
        assertThrows(IllegalArgumentException.class, () -> calculator.allowedRiskLevels(" "));
    }

    @Test
    void jointPolicyFiltersMissingRiskAndUsesNormalReasonWhenRetirementHasNoTdf() {
        when(investmentMapper.findAll())
                .thenReturn(List.of(product(1L, null, 100, false), product(2L, 5, null, false)));
        RecommendationContext context = new RecommendationContext();
        context.setInviterInvestmentType("STABLE_SEEKING");
        context.setInviteeInvestmentType("STABLE_SEEKING");
        context.setFirstGoalType("RETIREMENT");

        List<RecommendedProduct> result =
                new JointInvestmentRecommendationPolicy(investmentMapper, calculator)
                        .recommend(context);

        assertEquals(List.of(2L), result.stream().map(RecommendedProduct::productId).toList());
        assertEquals(RecommendationReasonCode.INVESTMENT_RISK_AND_AUM, result.get(0).reasonCode());
    }

    @Test
    void personalPolicyFiltersMissingRiskAndUsesNormalReasonWhenRetirementHasNoTdf() {
        when(investmentMapper.findAll())
                .thenReturn(List.of(product(1L, null, 100, false), product(2L, 5, null, false)));
        RecommendationContext context = new RecommendationContext();
        context.setInviterId(10L);
        context.setInviteeId(20L);
        context.setInviterInvestmentType("STABLE_SEEKING");
        context.setInviteeInvestmentType("STABLE");
        context.setFirstGoalType("RETIREMENT");

        List<RecommendedProduct> result =
                new PersonalInvestmentRecommendationPolicy(investmentMapper, calculator)
                        .recommend(context)
                        .get(10L);

        assertEquals(List.of(2L), result.stream().map(RecommendedProduct::productId).toList());
        assertEquals(RecommendationReasonCode.INVESTMENT_RISK_AND_AUM, result.get(0).reasonCode());
    }

    private InvestmentProduct product(Long id, Integer risk, Integer aum, boolean tdf) {
        InvestmentProduct product = new InvestmentProduct();
        product.setProductId(id);
        product.setRiskLevel(risk);
        product.setAum(aum);
        product.setIsTdf(tdf);
        return product;
    }
}
