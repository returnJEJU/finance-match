package com.financematch.recommendation.policy.joint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.config.RootConfig;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.RecommendedProduct;
import java.math.BigDecimal;
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
class SavingsRecommendationPolicyTest {

    @Autowired
    private SavingsRecommendationPolicy policy;

    @Test
    void 목표기간이_36개월이고_월가용금액이_100만원_이하이면_착한_eplus만_포함한다() {

        // given
        RecommendationContext context = new RecommendationContext();

        context.setTargetPeriodMonths(36);

        context.setInviterMonthlyAvailableAmount(
                BigDecimal.valueOf(300000));

        context.setInviteeMonthlyAvailableAmount(
                BigDecimal.valueOf(500000));

        // when
        List<RecommendedProduct> result =
                policy.recommend(context);

        // then
        assertEquals(3, result.size());

        RecommendedProduct first = result.get(0);

        assertEquals(6L, first.productId());
        assertEquals(1, first.rank());
        assertTrue(first.selected());

        RecommendedProduct second = result.get(1);

        assertEquals(4L, second.productId());
        assertEquals(2, second.rank());
        assertFalse(second.selected());

        RecommendedProduct third = result.get(2);

        assertEquals(3L, third.productId());
        assertEquals(3, third.rank());
        assertFalse(third.selected());
    }

    @Test
    void 월가용금액이_100만원_초과이면_일반_eplus도_후보에_포함한다() {

        // given
        RecommendationContext context = new RecommendationContext();
        context.setTargetPeriodMonths(36);
        context.setInviterMonthlyAvailableAmount(
                BigDecimal.valueOf(700_000L));
        context.setInviteeMonthlyAvailableAmount(
                BigDecimal.valueOf(500_000L));

        // when
        List<RecommendedProduct> result =
                policy.recommend(context);

        // then
        assertEquals(4, result.size());
        assertEquals(6L, result.get(0).productId());
        assertEquals(7L, result.get(1).productId());
        assertEquals(4L, result.get(2).productId());
        assertEquals(3L, result.get(3).productId());
    }

    @Test
    void KB스타적금_가입대상이면_12개월_이상에서_최우선_추천한다() {

        // given
        RecommendationContext context = new RecommendationContext();
        context.setTargetPeriodMonths(12);
        context.setInviterKbStarSavingsEligible(true);
        context.setInviterMonthlyAvailableAmount(
                BigDecimal.valueOf(300_000L));
        context.setInviteeMonthlyAvailableAmount(
                BigDecimal.valueOf(500_000L));

        // when
        List<RecommendedProduct> result =
                policy.recommend(context);

        // then
        RecommendedProduct first = result.get(0);
        assertEquals(5L, first.productId());
        assertEquals(1, first.rank());
        assertTrue(first.selected());
    }

    @Test
    void KB스타적금_가입대상이_아니면_후보에서_제외한다() {

        // given
        RecommendationContext context = new RecommendationContext();
        context.setTargetPeriodMonths(12);
        context.setInviterKbStarSavingsEligible(false);
        context.setInviteeKbStarSavingsEligible(false);
        context.setInviterMonthlyAvailableAmount(
                BigDecimal.valueOf(300_000L));
        context.setInviteeMonthlyAvailableAmount(
                BigDecimal.valueOf(500_000L));

        // when
        List<RecommendedProduct> result =
                policy.recommend(context);

        // then
        assertTrue(
                result.stream()
                        .noneMatch(product -> product.productId().equals(5L)));
    }

    @Test
    void 월가용금액이_최소납입금액보다_작으면_추천하지_않는다() {

        // given
        RecommendationContext context = new RecommendationContext();

        context.setTargetPeriodMonths(3);

        context.setInviterMonthlyAvailableAmount(
                BigDecimal.valueOf(200));

        context.setInviteeMonthlyAvailableAmount(
                BigDecimal.valueOf(300));

        // when
        List<RecommendedProduct> result =
                policy.recommend(context);

        // then
        assertTrue(result.isEmpty());
    }
}
