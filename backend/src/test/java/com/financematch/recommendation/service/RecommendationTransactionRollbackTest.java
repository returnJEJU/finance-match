package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.common.ErrorCode;
import com.financematch.config.RootConfig;
import com.financematch.exception.ApiException;
import com.financematch.recommendation.mapper.RecommendationMapper;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
            RootConfig.class,
            RecommendationTransactionRollbackTest.FailurePlannerConfig.class
        })
@WebAppConfiguration
class RecommendationTransactionRollbackTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long COUPLE_ID = 1L;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RecommendationMapper recommendationMapper;

    @Test
    void rollsBackAllSavedRowsWhenSavingRecommendationFails() {
        Long recommendationIdBefore =
                recommendationMapper.findRecommendationIdByMemberId(MEMBER_ID);
        int recommendationCountBefore =
                recommendationMapper.countRecommendationsByCoupleId(COUPLE_ID);
        int slotCountBefore = countSlots(recommendationIdBefore);
        int productCountBefore = countProducts(recommendationIdBefore);
        int taxSavingCountBefore =
                recommendationMapper.countPersonalTaxSavingByMemberId(MEMBER_ID);
        int investmentCountBefore =
                recommendationMapper.countPersonalInvestmentByMemberId(MEMBER_ID);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.createRecommendation(MEMBER_ID));

        assertEquals(ErrorCode.RECOMMENDATION_FAILED, exception.getErrorCode());
        assertEquals(
                recommendationCountBefore,
                recommendationMapper.countRecommendationsByCoupleId(COUPLE_ID));
        assertEquals(
                recommendationIdBefore,
                recommendationMapper.findRecommendationIdByMemberId(MEMBER_ID));
        assertEquals(slotCountBefore, countSlots(recommendationIdBefore));
        assertEquals(productCountBefore, countProducts(recommendationIdBefore));
        assertEquals(
                taxSavingCountBefore,
                recommendationMapper.countPersonalTaxSavingByMemberId(MEMBER_ID));
        assertEquals(
                investmentCountBefore,
                recommendationMapper.countPersonalInvestmentByMemberId(MEMBER_ID));
    }

    private int countSlots(Long recommendationId) {
        return recommendationId == null
                ? 0
                : recommendationMapper.countSlotsByRecommendationId(recommendationId);
    }

    private int countProducts(Long recommendationId) {
        return recommendationId == null
                ? 0
                : recommendationMapper.countProductsByRecommendationId(recommendationId);
    }

    static class FailurePlannerConfig {

        @Bean
        @Primary
        RecommendationPlanner failureRecommendationPlanner() {
            return new RecommendationPlanner(List.of(), List.of()) {
                @Override
                public RecommendationPlan create(
                        com.financematch.recommendation.domain.RecommendationContext context) {
                    return new RecommendationPlan(
                            Map.of(
                                    RecommendationSlotType.DEPOSIT,
                                    List.of(
                                            new RecommendedProduct(1L, 1, true),
                                            new RecommendedProduct(1L, 2, false))),
                            Map.of());
                }
            };
        }
    }
}
