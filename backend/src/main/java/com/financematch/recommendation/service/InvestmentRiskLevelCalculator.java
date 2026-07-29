package com.financematch.recommendation.service;

import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InvestmentRiskLevelCalculator {

    public Set<Integer> allowedRiskLevels(String investmentType) {
        if (investmentType == null || investmentType.isBlank()) {
            throw new IllegalArgumentException("투자 성향은 필수입니다.");
        }

        return switch (investmentType) {
            case "STABLE" -> Set.of(6);
            case "STABLE_SEEKING" -> Set.of(5, 6);
            case "NEUTRAL" -> Set.of(4, 5, 6);
            case "AGGRESSIVE" -> Set.of(3, 4, 5, 6);
            case "VERY_AGGRESSIVE" -> Set.of(1, 2, 3, 4, 5, 6);
            default ->
                    throw new IllegalArgumentException(
                            "지원하지 않는 투자 성향입니다: " + investmentType);
        };
    }

    public Set<Integer> conservativeAllowedRiskLevels(
            String firstInvestmentType, String secondInvestmentType) {
        Set<Integer> firstLevels = allowedRiskLevels(firstInvestmentType);
        Set<Integer> secondLevels = allowedRiskLevels(secondInvestmentType);

        int firstHighestRiskLevel = firstLevels.stream().min(Integer::compareTo).orElseThrow();
        int secondHighestRiskLevel = secondLevels.stream().min(Integer::compareTo).orElseThrow();

        return firstHighestRiskLevel >= secondHighestRiskLevel ? firstLevels : secondLevels;
    }
}
