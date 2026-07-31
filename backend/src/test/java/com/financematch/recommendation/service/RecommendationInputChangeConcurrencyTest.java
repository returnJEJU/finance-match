package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.common.ErrorCode;
import com.financematch.config.RootConfig;
import com.financematch.exception.ApiException;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.mapper.RecommendationFreshnessTestMapper;
import com.financematch.recommendation.mapper.RecommendationMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
            RootConfig.class,
            RecommendationInputChangeConcurrencyTest.BlockingPlannerConfig.class
        })
@WebAppConfiguration
class RecommendationInputChangeConcurrencyTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long COUPLE_ID = 1L;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private RecommendationMapper recommendationMapper;

    @Autowired
    private RecommendationFreshnessTestMapper recommendationFreshnessTestMapper;

    @Autowired
    private BlockingRecommendationPlanner blockingRecommendationPlanner;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void rollsBackRecommendationWhenFinancialSummaryChangesDuringGeneration() throws Exception {
        LocalDateTime originalFinancialUpdatedAt =
                recommendationFreshnessTestMapper
                        .findFinancialSummaryUpdatedAt(MEMBER_ID);
        assertNotNull(originalFinancialUpdatedAt);
        Long recommendationIdBefore =
                recommendationMapper.findRecommendationIdByMemberId(MEMBER_ID);
        int recommendationCountBefore =
                recommendationMapper.countRecommendationsByCoupleId(COUPLE_ID);
        int slotCountBefore = countSlots(recommendationIdBefore);
        int productCountBefore = countProducts(recommendationIdBefore);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ApiException> generation =
                executor.submit(
                        () ->
                                assertThrows(
                                        ApiException.class,
                                        () ->
                                                recommendationService
                                                        .createRecommendation(MEMBER_ID)));

        try {
            blockingRecommendationPlanner.awaitCalculation();
            inTransaction(
                    () ->
                            assertEquals(
                                    1,
                                    recommendationFreshnessTestMapper
                                            .moveFinancialSummaryToFuture(MEMBER_ID)));
            blockingRecommendationPlanner.continueSaving();

            ApiException exception = generation.get(5, TimeUnit.SECONDS);

            assertEquals(ErrorCode.RECOMMENDATION_FAILED, exception.getErrorCode());
            assertEquals(
                    recommendationCountBefore,
                    recommendationMapper.countRecommendationsByCoupleId(COUPLE_ID));
            assertEquals(
                    recommendationIdBefore,
                    recommendationMapper.findRecommendationIdByMemberId(MEMBER_ID));
            assertEquals(slotCountBefore, countSlots(recommendationIdBefore));
            assertEquals(productCountBefore, countProducts(recommendationIdBefore));
        } finally {
            blockingRecommendationPlanner.continueSaving();
            inTransaction(
                    () ->
                            recommendationFreshnessTestMapper
                                    .restoreFinancialSummaryUpdatedAt(
                                            MEMBER_ID,
                                            originalFinancialUpdatedAt));
            executor.shutdownNow();
        }
    }

    private void inTransaction(Runnable operation) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> operation.run());
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

    static class BlockingPlannerConfig {

        @Bean
        @Primary
        BlockingRecommendationPlanner blockingRecommendationPlanner() {
            return new BlockingRecommendationPlanner();
        }
    }

    static class BlockingRecommendationPlanner extends RecommendationPlanner {

        private final CountDownLatch calculationStarted = new CountDownLatch(1);
        private final CountDownLatch continueSaving = new CountDownLatch(1);

        private BlockingRecommendationPlanner() {
            super(List.of(), List.of());
        }

        @Override
        public RecommendationPlan create(RecommendationContext context) {
            calculationStarted.countDown();
            await(continueSaving, "추천 저장 재개 신호를 받지 못했습니다.");
            return new RecommendationPlan(Map.of(), Map.of());
        }

        private void awaitCalculation() {
            await(calculationStarted, "추천 계산 시작 신호를 받지 못했습니다.");
        }

        private void continueSaving() {
            continueSaving.countDown();
        }

        private void await(CountDownLatch latch, String timeoutMessage) {
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(timeoutMessage);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("추천 입력 변경 테스트가 중단되었습니다.", exception);
            }
        }
    }
}
