package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;

class InvestmentRiskLevelCalculatorTest {

    private final InvestmentRiskLevelCalculator calculator = new InvestmentRiskLevelCalculator();

    @Test
    void returnsAllowedRiskLevelsForEachInvestmentType() {
        assertEquals(Set.of(6), calculator.allowedRiskLevels("STABLE"));
        assertEquals(Set.of(5, 6), calculator.allowedRiskLevels("STABLE_SEEKING"));
        assertEquals(Set.of(4, 5, 6), calculator.allowedRiskLevels("NEUTRAL"));
        assertEquals(Set.of(3, 4, 5, 6), calculator.allowedRiskLevels("AGGRESSIVE"));
        assertEquals(
                Set.of(1, 2, 3, 4, 5, 6),
                calculator.allowedRiskLevels("VERY_AGGRESSIVE"));
    }

    @Test
    void rejectsMissingOrUnknownInvestmentType() {
        assertThrows(IllegalArgumentException.class, () -> calculator.allowedRiskLevels(null));
        assertThrows(IllegalArgumentException.class, () -> calculator.allowedRiskLevels("UNKNOWN"));
    }

    @Test
    void selectsMoreConservativeRiskLevelsForCouple() {
        assertEquals(
                Set.of(5, 6),
                calculator.conservativeAllowedRiskLevels("VERY_AGGRESSIVE", "STABLE_SEEKING"));
    }
}
