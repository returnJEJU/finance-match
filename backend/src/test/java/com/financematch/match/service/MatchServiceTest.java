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
import com.financematch.match.domain.CompatibilityResult;
import com.financematch.match.domain.MatchCoupleData;
import com.financematch.match.domain.MatchMemberData;
import com.financematch.match.mapper.MatchMapper;
import com.financematch.report.dto.reason.DebtRepaymentReasonInput;
import com.financematch.report.dto.reason.FinancialValueReasonInput;
import com.financematch.report.dto.reason.TaxStrategyReasonInput;
import com.financematch.report.service.AssetStabilityScoreService;
import com.financematch.report.service.DebtRepaymentScoreService;
import com.financematch.report.service.FinancialValueScoreService;
import com.financematch.report.service.GoalFeasibilityScoreService;
import com.financematch.report.service.TaxStrategyScoreService;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private MatchCalculationPersistenceService matchCalculationPersistenceService;

    @Mock
    private GoalFeasibilityScoreService goalFeasibilityScoreService;

    @Mock
    private AssetStabilityScoreService assetStabilityScoreService;

    @Mock
    private DebtRepaymentScoreService debtRepaymentScoreService;

    @Mock
    private FinancialValueScoreService financialValueScoreService;

    @Mock
    private TaxStrategyScoreService taxStrategyScoreService;

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

        verify(matchCalculationPersistenceService, never())
                .calculateAndPersist(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
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

        verify(financialValueScoreService, never())
                .generateAndSave(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any()
                );

        verify(taxStrategyScoreService, never())
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
                        .financialAssetRatioScore(3)
                        .investmentExperienceScore(2)
                        .financialKnowledgeScore(4)
                        .capitalPreservationScore(5)
                        .hasIsa(true)
                        .isaAnnualDeposit(new BigDecimal("12000000"))
                        .hasIrp(true)
                        .irpAnnualPayment(new BigDecimal("3000000"))
                        .dcAnnualPayment(new BigDecimal("1000000"))
                        .hasPensionSaving(false)
                        .pensionAnnualPayment(BigDecimal.ZERO)
                        .build();

        MemberCalculationInput memberBCalcInput =
                MemberCalculationInput.builder()
                        .totalDebt(BigDecimal.ZERO)
                        .financialAssetRatioScore(3)
                        .investmentExperienceScore(2)
                        .financialKnowledgeScore(1)
                        .capitalPreservationScore(2)
                        .hasIsa(false)
                        .isaAnnualDeposit(BigDecimal.ZERO)
                        .hasIrp(true)
                        .irpAnnualPayment(new BigDecimal("5000000"))
                        .dcAnnualPayment(BigDecimal.ZERO)
                        .hasPensionSaving(true)
                        .pensionAnnualPayment(new BigDecimal("6000000"))
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

        // 캐시 확인 시점에는 결과가 없다. 계산·저장은 MatchCalculationPersistenceService 로 위임된다.
        when(matchMapper.findCompatibilityResultByCoupleId(1L))
                .thenReturn(null);

        when(matchMapper.findMemberDataByMemberId(1L))
                .thenReturn(memberA);

        when(matchMapper.findMemberDataByMemberId(2L))
                .thenReturn(memberB);

        when(matchCalculationPersistenceService.calculateAndPersist(couple, memberA, memberB))
                .thenReturn(new MatchCalculationPersistenceResult(savedResult, calculationInput, calculationResult));

        CompatibilityResult result =
                matchService.getOrCalculateCompatibilityResult(1L);

        assertSame(savedResult, result);

        verify(matchMapper).findMemberDataByMemberId(1L);
        verify(matchMapper).findMemberDataByMemberId(2L);

        verify(matchCalculationPersistenceService).calculateAndPersist(couple, memberA, memberB);

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

        // 투자 가치관 일치도 reason 생성이 두 회원의 설문 4문항 원점수를 정확히 담아 호출됐는지 확인
        ArgumentCaptor<FinancialValueReasonInput> financialValueInputCaptor =
                ArgumentCaptor.forClass(FinancialValueReasonInput.class);
        verify(financialValueScoreService).generateAndSave(
                org.mockito.ArgumentMatchers.eq(10L), financialValueInputCaptor.capture());

        FinancialValueReasonInput capturedFinancialValueInput = financialValueInputCaptor.getValue();
        assertEquals(3, capturedFinancialValueInput.getMeAssetRatio());
        assertEquals(2, capturedFinancialValueInput.getMeInvestExperience());
        assertEquals(4, capturedFinancialValueInput.getMeProductUnderstanding());
        assertEquals(5, capturedFinancialValueInput.getMeLossTolerance());
        assertEquals(3, capturedFinancialValueInput.getPartnerAssetRatio());
        assertEquals(2, capturedFinancialValueInput.getPartnerInvestExperience());
        assertEquals(1, capturedFinancialValueInput.getPartnerProductUnderstanding());
        assertEquals(2, capturedFinancialValueInput.getPartnerLossTolerance());

        // 절세 축 reason 생성이 두 회원의 계좌 개설 여부·납입액·한도를 정확히 담아 호출됐는지 확인
        ArgumentCaptor<TaxStrategyReasonInput> taxInputCaptor =
                ArgumentCaptor.forClass(TaxStrategyReasonInput.class);
        verify(taxStrategyScoreService).generateAndSave(
                org.mockito.ArgumentMatchers.eq(10L), taxInputCaptor.capture());

        TaxStrategyReasonInput capturedTaxInput = taxInputCaptor.getValue();
        assertEquals("김철수", capturedTaxInput.getMeName());
        assertEquals("이영희", capturedTaxInput.getPartnerName());

        assertEquals(true, capturedTaxInput.getMe().getIsa().isOpened());
        assertEquals(new BigDecimal("12000000"), capturedTaxInput.getMe().getIsa().getContributed());
        assertEquals(MatchCalculator.ISA_ANNUAL_LIMIT_AMOUNT, capturedTaxInput.getMe().getIsa().getAnnualLimit());

        assertEquals(true, capturedTaxInput.getMe().getIrp().isOpened());
        assertEquals(new BigDecimal("4000000"), capturedTaxInput.getMe().getIrp().getContributed());
        assertEquals(MatchCalculator.PENSION_IRP_ANNUAL_LIMIT, capturedTaxInput.getMe().getIrp().getAnnualLimit());

        assertEquals(false, capturedTaxInput.getMe().getPension().isOpened());

        assertEquals(false, capturedTaxInput.getPartner().getIsa().isOpened());
        assertEquals(true, capturedTaxInput.getPartner().getPension().isOpened());
        assertEquals(new BigDecimal("6000000"), capturedTaxInput.getPartner().getPension().getContributed());
        assertEquals(
                MatchCalculator.PENSION_SAVING_ANNUAL_LIMIT,
                capturedTaxInput.getPartner().getPension().getAnnualLimit());
    }
}