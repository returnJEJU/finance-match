package com.financematch.recommendation.controller;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.common.ApiResponse;
import com.financematch.recommendation.dto.RecommendationResponse;
import com.financematch.recommendation.service.RecommendationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private RecommendationController recommendationController;

    @Test
    void createsRecommendationAndReturnsNullData() {
        ApiResponse<Void> response =
                recommendationController.createRecommendation(1L);

        verify(recommendationService).createRecommendation(1L);
        assertTrue(response.isSuccess());
        assertNull(response.getData());
    }

    @Test
    void returnsRecommendationForLoginMember() {
        RecommendationResponse recommendation =
                new RecommendationResponse(100L, "신혼집 스타터 패키지", false, List.of(), null, null);
        when(recommendationService.getRecommendation(1L))
                .thenReturn(recommendation);

        ApiResponse<RecommendationResponse> response =
                recommendationController.getRecommendation(1L);

        verify(recommendationService).getRecommendation(1L);
        assertTrue(response.isSuccess());
        assertSame(recommendation, response.getData());
    }
}
