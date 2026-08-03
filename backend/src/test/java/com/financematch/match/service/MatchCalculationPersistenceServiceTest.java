package com.financematch.match.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MatchCalculationResult;
import com.financematch.match.calculator.MatchCalculator;
import com.financematch.match.converter.MatchCalculationInputConverter;
import com.financematch.match.domain.CompatibilityResult;
import com.financematch.match.domain.MatchCoupleData;
import com.financematch.match.domain.MatchMemberData;
import com.financematch.match.mapper.MatchMapper;

@ExtendWith(MockitoExtension.class)
class MatchCalculationPersistenceServiceTest {

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private MatchCalculationInputConverter converter;

    @Mock
    private MatchCalculator calculator;

    @InjectMocks
    private MatchCalculationPersistenceService service;

    @Test
    void 계산_후_저장하고_저장된_결과를_다시_조회해서_반환한다() {
        MatchCoupleData couple = new MatchCoupleData();
        couple.setCoupleId(1L);

        MatchMemberData memberA = new MatchMemberData();
        MatchMemberData memberB = new MatchMemberData();

        MatchCalculationInput calculationInput = MatchCalculationInput.builder().build();
        MatchCalculationResult calculationResult =
                MatchCalculationResult.builder().totalScore(new BigDecimal("70.36")).build();

        CompatibilityResult savedResult = new CompatibilityResult();
        savedResult.setId(10L);

        when(converter.convert(couple, memberA, memberB)).thenReturn(calculationInput);
        when(calculator.calculate(calculationInput)).thenReturn(calculationResult);
        when(matchMapper.insertCompatibilityResult(1L, calculationResult)).thenReturn(1);
        when(matchMapper.findCompatibilityResultByCoupleId(1L)).thenReturn(savedResult);

        MatchCalculationPersistenceResult result = service.calculateAndPersist(couple, memberA, memberB);

        assertSame(savedResult, result.savedResult());
        assertSame(calculationInput, result.calculationInput());
        assertSame(calculationResult, result.calculationResult());

        verify(matchMapper).insertCompatibilityResult(1L, calculationResult);
    }

    @Test
    void 저장에_실패하면_예외를_던진다() {
        MatchCoupleData couple = new MatchCoupleData();
        couple.setCoupleId(1L);

        MatchMemberData memberA = new MatchMemberData();
        MatchMemberData memberB = new MatchMemberData();

        MatchCalculationInput calculationInput = MatchCalculationInput.builder().build();
        MatchCalculationResult calculationResult = MatchCalculationResult.builder().build();

        when(converter.convert(couple, memberA, memberB)).thenReturn(calculationInput);
        when(calculator.calculate(calculationInput)).thenReturn(calculationResult);
        when(matchMapper.insertCompatibilityResult(1L, calculationResult)).thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> service.calculateAndPersist(couple, memberA, memberB));
    }
}
