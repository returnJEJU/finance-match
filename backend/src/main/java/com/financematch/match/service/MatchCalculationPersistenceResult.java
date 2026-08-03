package com.financematch.match.service;

import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MatchCalculationResult;
import com.financematch.match.domain.CompatibilityResult;

/** {@link MatchCalculationPersistenceService}가 짧은 트랜잭션에서 계산·저장한 결과 묶음. */
public record MatchCalculationPersistenceResult(
        CompatibilityResult savedResult,
        MatchCalculationInput calculationInput,
        MatchCalculationResult calculationResult) {}
