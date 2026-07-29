package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.config.RootConfig;
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
class RecommendationServiceTest {

    @Autowired
    private RecommendationService recommendationService;

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
}