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
class DepositRecommendationPolicyTest {

    @Autowired
    private DepositRecommendationPolicy policy;

    @Test
    void 목표기간이_12개월이면_적용금리가_높은_예금부터_추천한다() {

        // given
        RecommendationContext context = new RecommendationContext();
        context.setTargetPeriodMonths(12);
        context.setAvailableBalance(
                BigDecimal.valueOf(9_100_000L));

        // when
        List<RecommendedProduct> result =
                policy.recommend(context);

        // then
        assertEquals(2, result.size());

        RecommendedProduct first = result.get(0);
        assertEquals(2L, first.productId());
        assertEquals(1, first.rank());
        assertTrue(first.selected());

        RecommendedProduct second = result.get(1);
        assertEquals(1L, second.productId());
        assertEquals(2, second.rank());
        assertFalse(second.selected());
    }

    @Test
    void 목표기간이_2개월이면_가입가능한_예금만_추천한다() {

        // given
        RecommendationContext context = new RecommendationContext();
        context.setTargetPeriodMonths(2);
        context.setAvailableBalance(
                BigDecimal.valueOf(9_100_000L));

        // when
        List<RecommendedProduct> result =
                policy.recommend(context);

        // then
        assertEquals(1, result.size());

        RecommendedProduct first = result.get(0);

        assertEquals(1L, first.productId());
        assertEquals(1, first.rank());
        assertTrue(first.selected());
    }

    @Test
    void 입출금잔액합계가_910만원_미만이면_예금을_추천하지_않는다() {

        // given
        RecommendationContext context = new RecommendationContext();
        context.setTargetPeriodMonths(12);
        context.setAvailableBalance(
                BigDecimal.valueOf(9_099_999L));

        // when
        List<RecommendedProduct> result =
                policy.recommend(context);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void 입출금잔액합계가_정확히_910만원이면_예금을_추천한다() {

        // given
        RecommendationContext context = new RecommendationContext();
        context.setTargetPeriodMonths(12);
        context.setAvailableBalance(
                BigDecimal.valueOf(9_100_000L));

        // when
        List<RecommendedProduct> result =
                policy.recommend(context);

        // then
        assertFalse(result.isEmpty());
    }
}