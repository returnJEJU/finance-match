package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.common.ErrorCode;
import com.financematch.config.RootConfig;
import com.financematch.exception.ApiException;
import com.financematch.recommendation.mapper.RecommendationMapper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@WebAppConfiguration
class RecommendationConcurrencyTest {

    private static final Long MEMBER_ID = 1L;

    @Autowired
    private RecommendationMapper recommendationMapper;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void rejectsSecondGenerationRequestWhenCoupleRowIsLocked()
            throws Exception {

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        Future<?> lockHolder =
                executor.submit(
                        () -> {
                            TransactionTemplate transaction =
                                    new TransactionTemplate(
                                            transactionManager);

                            transaction.executeWithoutResult(
                                    status -> {
                                        Long coupleId =
                                                recommendationMapper
                                                        .findCoupleIdByMemberId(
                                                                MEMBER_ID);

                                        recommendationMapper
                                                .lockCoupleIdByIdNowait(
                                                        coupleId);

                                        locked.countDown();
                                        awaitRelease(release);
                                    });
                        });

        try {
            if (!locked.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "커플 행 잠금을 제한 시간 안에 획득하지 못했습니다.");
            }

            ApiException exception =
                    assertThrows(
                            ApiException.class,
                            () ->
                                    recommendationService
                                            .createRecommendation(
                                                    MEMBER_ID));

            assertEquals(
                    ErrorCode.RECOMMENDATION_IN_PROGRESS,
                    exception.getErrorCode());
        } finally {
            release.countDown();
            lockHolder.get(5, TimeUnit.SECONDS);
            executor.shutdownNow();
        }
    }

    private void awaitRelease(CountDownLatch release) {
        try {
            if (!release.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "커플 행 잠금 해제 신호를 받지 못했습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "커플 행 잠금 테스트가 중단되었습니다.",
                    exception);
        }
    }
}
