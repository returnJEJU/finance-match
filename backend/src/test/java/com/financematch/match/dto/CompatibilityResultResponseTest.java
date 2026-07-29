package com.financematch.match.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.financematch.match.domain.CompatibilityResult;

class CompatibilityResultResponseTest {

    @Test
    void 도메인_결과를_응답_DTO로_변환한다() {

        CompatibilityResult result =
                new CompatibilityResult();

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
                new BigDecimal("0.00")
        );
        result.setTaxStrategyCalculated(false);
        result.setTotalScore(
                new BigDecimal("69.29")
        );
        result.setResultSummary(null);

        CompatibilityResultResponse response =
                CompatibilityResultResponse.from(result);

        assertEquals(1L, response.getCoupleId());
        assertEquals(
                new BigDecimal("18.62"),
                response.getAssetStabilityScore()
        );
        assertEquals(
                new BigDecimal("14.00"),
                response.getDebtRepaymentScore()
        );
        assertEquals(
                new BigDecimal("16.95"),
                response.getFinancialValueScore()
        );
        assertEquals(
                new BigDecimal("12.79"),
                response.getGoalFeasibilityScore()
        );
        assertEquals(
                new BigDecimal("0.00"),
                response.getTaxStrategyScore()
        );
        assertFalse(response.isTaxStrategyCalculated());
        assertEquals(
                new BigDecimal("69.29"),
                response.getTotalScore()
        );
    }
}