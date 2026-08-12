package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.financematch.common.ErrorCode;
import com.financematch.couple.mapper.CoupleMapper;
import com.financematch.exception.ApiException;
import com.financematch.recommendation.domain.Recommendation;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.mapper.RecommendationContextMapper;
import com.financematch.recommendation.mapper.RecommendationMapper;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceCreationTest {

    @Mock
    private RecommendationContextMapper recommendationContextMapper;

    @Mock
    private RecommendationMapper recommendationMapper;

    @Mock
    private RecommendationPlanner recommendationPlanner;

    @Mock
    private CoupleMapper coupleMapper;

    @Mock
    private RecommendationResponseAssembler responseAssembler;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void rejectsConcurrentRecommendationGenerationWithoutRunningPolicies() {
        when(recommendationMapper.findCoupleIdByMemberId(1L))
                .thenReturn(10L);

        when(recommendationMapper.lockCoupleIdByIdNowait(10L))
                .thenThrow(
                        new CannotAcquireLockException(
                                "couple row is locked"));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                recommendationService
                                        .createRecommendation(1L));

        assertEquals(
                ErrorCode.RECOMMENDATION_IN_PROGRESS,
                exception.getErrorCode());

        verifyNoInteractions(
                recommendationContextMapper,
                recommendationPlanner);
    }

    @Test
    void returnsCoupleNotConnectedBeforeRunningPolicies() {
        when(recommendationMapper.findCoupleIdByMemberId(1L))
                .thenReturn(null);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                recommendationService
                                        .createRecommendation(1L));

        assertEquals(
                ErrorCode.COUPLE_NOT_CONNECTED,
                exception.getErrorCode());

        verifyNoInteractions(
                recommendationContextMapper,
                recommendationPlanner);
    }

    @Test
    void returnsNotReadyWhenRecommendationInputIsInvalid() {
        LocalDateTime inputVersion = LocalDateTime.of(2026, 8, 12, 10, 0);

        when(recommendationMapper.findCoupleIdByMemberId(1L)).thenReturn(10L);
        when(recommendationMapper.lockCoupleIdByIdNowait(10L)).thenReturn(10L);
        when(recommendationMapper.findLatestInputUpdatedAtByMemberId(1L))
                .thenReturn(inputVersion);
        when(recommendationContextMapper.findByMemberId(1L))
                .thenThrow(new IllegalArgumentException("추천 입력정보가 올바르지 않습니다."));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.createRecommendation(1L));

        assertEquals(ErrorCode.RECOMMENDATION_NOT_READY, exception.getErrorCode());
        verifyNoInteractions(recommendationPlanner);
    }

    @Test
    void rejectsResultWhenRecommendationInputChangesDuringGeneration() {
        LocalDateTime before =
                LocalDateTime.of(2026, 7, 30, 10, 0);

        LocalDateTime after =
                before.plusSeconds(1);

        RecommendationContext context =
                new RecommendationContext();

        context.setCoupleId(10L);
        context.setInviterId(1L);
        context.setInviteeId(2L);

        when(recommendationMapper.findCoupleIdByMemberId(1L))
                .thenReturn(10L);

        when(recommendationMapper.lockCoupleIdByIdNowait(10L))
                .thenReturn(10L);

        when(
                recommendationMapper
                        .findLatestInputUpdatedAtByMemberId(1L))
                .thenReturn(before, after);

        when(recommendationContextMapper.findByMemberId(1L))
                .thenReturn(context);

        when(recommendationPlanner.create(context))
                .thenReturn(
                        new RecommendationPlan(
                                Map.of(),
                                Map.of()));

        doAnswer(
                invocation -> {
                    Recommendation recommendation =
                            invocation.getArgument(0);

                    recommendation.setId(100L);
                    return 1;
                })
                .when(recommendationMapper)
                .upsertRecommendation(
                        any(Recommendation.class));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                recommendationService
                                        .createRecommendation(1L));

        assertEquals(
                ErrorCode.RECOMMENDATION_FAILED,
                exception.getErrorCode());
    }
}