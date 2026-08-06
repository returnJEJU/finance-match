package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.config.RootConfig;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.service.RecommendationPlan;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@WebAppConfiguration
class RecommendationPlannerTest {

    @Autowired
    private RecommendationPlanner recommendationPlanner;

    @Test
    void 공동_예금_추천결과를_플랜에_담는다() {

        // given
        RecommendationContext context = new RecommendationContext();
        context.setInviterId(1L);
        context.setInviteeId(2L);
        context.setInviterInvestmentType("STABLE");
        context.setInviteeInvestmentType("STABLE");
        context.setInviterFinancialKnowledge("LOW");
        context.setInviteeFinancialKnowledge("LOW");

        context.setInviterTaxEligibilityStatus("ELIGIBLE");
        context.setInviterIsaEligibilityStatus("ELIGIBLE");
        context.setInviteeTaxEligibilityStatus("ELIGIBLE");
        context.setInviteeIsaEligibilityStatus("ELIGIBLE");

        context.setTargetPeriodMonths(12);
        context.setAvailableBalance(
                java.math.BigDecimal.valueOf(9_100_000L));

        // when
        RecommendationPlan plan =
                recommendationPlanner.create(context);

        // then
        assertNotNull(plan);

        assertTrue(
                plan.joint()
                        .containsKey(RecommendationSlotType.DEPOSIT)
        );

        List<RecommendedProduct> deposits =
                plan.joint()
                        .get(RecommendationSlotType.DEPOSIT);

        assertEquals(2, deposits.size());

        RecommendedProduct first = deposits.get(0);

        assertEquals(2L, first.productId());
        assertEquals(1, first.rank());
        assertTrue(first.selected());
    }
}
