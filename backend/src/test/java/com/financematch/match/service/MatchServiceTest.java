package com.financematch.match.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MatchCalculationResult;
import com.financematch.match.calculator.MatchCalculator;
import com.financematch.match.calculator.MemberCalculationInput;
import com.financematch.match.converter.MatchCalculationInputConverter;
import com.financematch.match.domain.CompatibilityResult;
import com.financematch.match.domain.MatchCoupleData;
import com.financematch.match.domain.MatchMemberData;
import com.financematch.match.mapper.MatchMapper;
import com.financematch.report.dto.reason.DebtRepaymentReasonInput;
import com.financematch.report.service.AssetStabilityScoreService;
import com.financematch.report.service.DebtRepaymentScoreService;
import com.financematch.report.service.GoalFeasibilityScoreService;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private MatchCalculationInputConverter converter;

    @Mock
    private MatchCalculator calculator;

    @Mock
    private GoalFeasibilityScoreService goalFeasibilityScoreService;

    @Mock
    private AssetStabilityScoreService assetStabilityScoreService;

    @Mock
    private DebtRepaymentScoreService debtRepaymentScoreService;

    @InjectMocks
    private MatchService matchService;

    @Test
    void 기존_결과가_있으면_재계산하지_않고_반환한다() {

        MatchCoupleData couple = new MatchCoupleData();
        couple.setCoupleId(1L);
        couple.setInviterId(1L);
        couple.setInviteeId(2L);

        CompatibilityResult existingResult =
                new CompatibilityResult();

        existingResult.setCoupleId(1L);
        existingResult.setTotalScore(new BigDecimal("73.15"));

        when(matchMapper.findCoupleDataByMemberId(1L))
                .thenReturn(couple);

        when(matchMapper.findCompatibilityResultByCoupleId(1L))
                .thenReturn(existingResult);

        CompatibilityResult result =
                matchService.getOrCalculateCompatibilityResult(1L);

        assertSame(existingResult, result);

        verify(matchMapper, never())
                .findMemberDataByMemberId(anyLong());

        verify(converter, never())
                .convert(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );

        verify(calculator, never())
                .calculate(org.mockito.ArgumentMatchers.any());

        verify(matchMapper, never())
                .insertCompatibilityResult(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any()
                );

        verify(goalFeasibilityScoreService, never())
                .generateAndSave(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt()
                );

        verify(assetStabilityScoreService, never())
                .generateAndSave(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyDouble()
                );

        verify(debtRepaymentScoreService, never())
                .generateAndSave(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void 기존_결과가_없으면_계산하고_최초_저장한다() {

        MatchCoupleData couple = new MatchCoupleData();
        couple.setCoupleId(1L);
        couple.setInviterId(1L);
        couple.setInviteeId(2L);
        couple.setFirstGoalType("HOUSING");
        couple.setTargetAmount(new BigDecimal("350000000"));
        couple.setTargetPeriodMonths(36);

        MatchMemberData memberA = new MatchMemberData();
        memberA.setMemberId(1L);
        memberA.setMemberName("김철수");

        MatchMemberData memberB = new MatchMemberData();
        memberB.setMemberId(2L);
        memberB.setMemberName("이영희");

        MemberCalculationInput memberACalcInput =
                MemberCalculationInput.builder()
                        .totalDebt(new BigDecimal("1000000"))
                        .build();

        MemberCalculationInput memberBCalcInput =
                MemberCalculationInput.builder()
                        .totalDebt(BigDecimal.ZERO)
                        .build();

        MatchCalculationInput calculationInput =
                MatchCalculationInput.builder()
                        .memberA(memberACalcInput)
                        .memberB(memberBCalcInput)
                        .targetAmount(new BigDecimal("350000000"))
                        .targetPeriodMonths(36)
                        .build();

        MatchCalculationResult calculationResult =
                MatchCalculationResult.builder()
                        .assetStabilityScore(new BigDecimal("18.62"))
                        .coupleAssetRatio(1.1)
                        .debtRepaymentScore(new BigDecimal("14.00"))
                        .memberADebtScore(new BigDecimal("62.50"))
                        .memberBDebtScore(new BigDecimal("100.00"))
                        .financialValueScore(new BigDecimal("16.95"))
                        .goalFeasibilityScore(new BigDecimal("12.79"))
                        .expectedAsset(new BigDecimal("362000000"))
                        .taxStrategyScore(new BigDecimal("8.00"))
                        .taxStrategyCalculated(true)
                        .totalScore(new BigDecimal("70.36"))
                        .build();

        CompatibilityResult savedResult =
                new CompatibilityResult();

        savedResult.setId(10L);
        savedResult.setCoupleId(1L);
        savedResult.setTotalScore(new BigDecimal("70.36"));

        when(matchMapper.findCoupleDataByMemberId(1L))
                .thenReturn(couple);

        // 첫 번째 조회에는 결과가 없고,
        // 저장 후 두 번째 조회에는 결과가 존재한다.
        when(matchMapper.findCompatibilityResultByCoupleId(1L))
                .thenReturn(null, savedResult);

        when(matchMapper.findMemberDataByMemberId(1L))
                .thenReturn(memberA);

        when(matchMapper.findMemberDataByMemberId(2L))
                .thenReturn(memberB);

        when(converter.convert(couple, memberA, memberB))
                .thenReturn(calculationInput);

        when(calculator.calculate(calculationInput))
                .thenReturn(calculationResult);

        when(matchMapper.insertCompatibilityResult(
                1L,
                calculationResult
        )).thenReturn(1);

        CompatibilityResult result =
                matchService.getOrCalculateCompatibilityResult(1L);

        assertSame(savedResult, result);

        verify(matchMapper).findMemberDataByMemberId(1L);
        verify(matchMapper).findMemberDataByMemberId(2L);

        verify(converter).convert(couple, memberA, memberB);
        verify(calculator).calculate(calculationInput);

        verify(matchMapper).insertCompatibilityResult(
                1L,
                calculationResult
        );

        // 저장 전 한 번, 저장 후 한 번
        verify(
                matchMapper,
                org.mockito.Mockito.times(2)
        ).findCompatibilityResultByCoupleId(1L);

        // 목표 달성 가능성 reason 생성이 저장된 결과의 id·계산기 산출값으로 정확히 호출됐는지 확인
        verify(goalFeasibilityScoreService).generateAndSave(
                10L,
                new BigDecimal("362000000"),
                new BigDecimal("350000000"),
                36
        );

        // 금융 자산 축 reason 생성도 저장된 결과의 id·ratio로 정확히 호출됐는지 확인
        verify(assetStabilityScoreService).generateAndSave(10L, 1.1);

        // 부채 축 reason 생성이 이름·부채 유무·점수를 정확히 담아 호출됐는지 확인
        ArgumentCaptor<DebtRepaymentReasonInput> debtInputCaptor =
                ArgumentCaptor.forClass(DebtRepaymentReasonInput.class);
        verify(debtRepaymentScoreService).generateAndSave(
                org.mockito.ArgumentMatchers.eq(10L), debtInputCaptor.capture());

        DebtRepaymentReasonInput capturedDebtInput = debtInputCaptor.getValue();
        assertEquals("김철수", capturedDebtInput.getMeName());
        assertEquals("이영희", capturedDebtInput.getPartnerName());
        assertEquals(true, capturedDebtInput.isMeHasDebt());
        assertEquals(false, capturedDebtInput.isPartnerHasDebt());
        assertEquals(new BigDecimal("62.50"), capturedDebtInput.getMeScore());
        assertEquals(new BigDecimal("100.00"), capturedDebtInput.getPartnerScore());
    }
}