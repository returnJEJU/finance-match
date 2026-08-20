package com.financematch.match.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.financematch.common.ApiResponse;
import com.financematch.match.domain.CompatibilityResult;
import com.financematch.match.dto.CompatibilityResultResponse;
import com.financematch.match.service.MatchService;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {

    @Mock
    private MatchService matchService;

    @InjectMocks
    private MatchController matchController;

    @Test
    void 궁합도를_계산하고_성공응답으로_반환한다() {
        // given
        Long memberId = 1L;
        CompatibilityResult serviceResult = createCompatibilityResult();

        when(matchService.getOrCalculateCompatibilityResult(memberId))
                .thenReturn(serviceResult);

        // when
        ApiResponse<CompatibilityResultResponse> response =
                matchController.calculateCompatibility(memberId);

        // then
        verify(matchService)
                .getOrCalculateCompatibilityResult(memberId);

        assertTrue(response.isSuccess());

        CompatibilityResultResponse data = response.getData();

        assertEquals(1L, data.getCoupleId());
        assertEquals(
                new BigDecimal("18.62"),
                data.getAssetStabilityScore()
        );
        assertEquals(
                new BigDecimal("14.00"),
                data.getDebtRepaymentScore()
        );
        assertEquals(
                new BigDecimal("16.95"),
                data.getFinancialValueScore()
        );
        assertEquals(
                new BigDecimal("12.79"),
                data.getGoalFeasibilityScore()
        );
        assertEquals(
                new BigDecimal("8.00"),
                data.getTaxStrategyScore()
        );
        assertTrue(data.isTaxStrategyCalculated());
        assertEquals(
                new BigDecimal("70.36"),
                data.getTotalScore()
        );
        assertEquals(
                "두 사람의 금융 궁합이 좋은 편입니다.",
                data.getResultSummary()
        );
    }

    @Test
    void 저장된_궁합도를_조회하고_성공응답으로_반환한다() {
        // given
        Long memberId = 1L;
        CompatibilityResult serviceResult = createCompatibilityResult();

        when(matchService.getCompatibilityResult(memberId))
                .thenReturn(serviceResult);

        // when
        ApiResponse<CompatibilityResultResponse> response =
                matchController.getCompatibility(memberId);

        // then
        verify(matchService).getCompatibilityResult(memberId);

        assertTrue(response.isSuccess());

        CompatibilityResultResponse data = response.getData();

        assertEquals(serviceResult.getCoupleId(), data.getCoupleId());
        assertEquals(
                serviceResult.getAssetStabilityScore(),
                data.getAssetStabilityScore()
        );
        assertEquals(
                serviceResult.getDebtRepaymentScore(),
                data.getDebtRepaymentScore()
        );
        assertEquals(
                serviceResult.getFinancialValueScore(),
                data.getFinancialValueScore()
        );
        assertEquals(
                serviceResult.getGoalFeasibilityScore(),
                data.getGoalFeasibilityScore()
        );
        assertEquals(
                serviceResult.getTaxStrategyScore(),
                data.getTaxStrategyScore()
        );
        assertEquals(
                serviceResult.isTaxStrategyCalculated(),
                data.isTaxStrategyCalculated()
        );
        assertEquals(
                serviceResult.getTotalScore(),
                data.getTotalScore()
        );
        assertEquals(
                serviceResult.getResultSummary(),
                data.getResultSummary()
        );
    }

    private CompatibilityResult createCompatibilityResult() {
        CompatibilityResult result = new CompatibilityResult();

        result.setId(10L);
        result.setCoupleId(1L);
        result.setAssetStabilityScore(
                new BigDecimal("18.62")
        );
        result.setDebtRepaymentScore(
                new BigDecimal("14.00")
        );
        result.setFinancialValueScore(
                new BigDecimal("16.95")
        );
        result.setGoalFeasibilityScore(
                new BigDecimal("12.79")
        );
        result.setTaxStrategyScore(
                new BigDecimal("8.00")
        );
        result.setTaxStrategyCalculated(true);
        result.setTotalScore(
                new BigDecimal("70.36")
        );
        result.setResultSummary(
                "두 사람의 금융 궁합이 좋은 편입니다."
        );

        return result;
    }
}