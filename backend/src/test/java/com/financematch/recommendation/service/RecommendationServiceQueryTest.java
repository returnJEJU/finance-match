package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.financematch.common.ErrorCode;
import com.financematch.couple.mapper.CoupleMapper;
import com.financematch.exception.ApiException;
import com.financematch.recommendation.domain.RecommendationResult;
import com.financematch.recommendation.dto.RecommendationResponse;
import com.financematch.recommendation.mapper.RecommendationContextMapper;
import com.financematch.recommendation.mapper.RecommendationMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceQueryTest {

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
    void returnsCurrentStoredRecommendation() {
        RecommendationResult result = new RecommendationResult();
        result.setRecommendationId(100L);
        RecommendationResponse expected =
                new RecommendationResponse(100L, "신혼집 스타터 패키지", false, List.of(), null, null);

        when(recommendationMapper.findRecommendationResultByMemberId(1L))
                .thenReturn(result);
        when(recommendationMapper.isRecommendationCurrentByMemberId(1L))
                .thenReturn(true);
        when(recommendationMapper.findJointProductsByRecommendationIdAndMemberId(100L, 1L))
                .thenReturn(List.of());
        when(recommendationMapper.findPersonalTaxSavingByMemberId(1L))
                .thenReturn(List.of());
        when(recommendationMapper.findPersonalInvestmentByMemberId(1L))
                .thenReturn(List.of());
        when(responseAssembler.assemble(result, List.of(), List.of(), List.of()))
                .thenReturn(expected);

        RecommendationResponse actual =
                recommendationService.getRecommendation(1L);

        assertSame(expected, actual);
    }

    @Test
    void returnsNotFoundWhenStoredRecommendationDoesNotExist() {
        when(recommendationMapper.findRecommendationResultByMemberId(1L))
                .thenReturn(null);
        when(coupleMapper.existsCoupleByMemberId(1L))
                .thenReturn(true);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.getRecommendation(1L));

        assertEquals(ErrorCode.RECOMMENDATION_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(responseAssembler);
    }

    @Test
    void returnsNotFoundWhenStoredRecommendationIsStale() {
        RecommendationResult result = new RecommendationResult();
        result.setRecommendationId(100L);
        when(recommendationMapper.findRecommendationResultByMemberId(1L))
                .thenReturn(result);
        when(recommendationMapper.isRecommendationCurrentByMemberId(1L))
                .thenReturn(false);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.getRecommendation(1L));

        assertEquals(ErrorCode.RECOMMENDATION_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(responseAssembler);
    }

    @Test
    void returnsCoupleNotConnectedWhenMemberHasNoCouple() {
        when(recommendationMapper.findRecommendationResultByMemberId(1L))
                .thenReturn(null);
        when(coupleMapper.existsCoupleByMemberId(1L))
                .thenReturn(false);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.getRecommendation(1L));

        assertEquals(ErrorCode.COUPLE_NOT_CONNECTED, exception.getErrorCode());
    }

    @Test
    void returnsFailedWhenRecommendationQueryRaisesUnexpectedError() {
        when(recommendationMapper.findRecommendationResultByMemberId(1L))
                .thenThrow(new IllegalStateException("DB 조회 실패"));

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> recommendationService.getRecommendation(1L));

        assertEquals(ErrorCode.RECOMMENDATION_FAILED, exception.getErrorCode());
        verifyNoInteractions(responseAssembler);
    }
}
