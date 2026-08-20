package com.financematch.match.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchCalculatorTest {

    @Mock
    private FinancialAssetEngine financialAssetEngine;

    @Mock
    private DebtRepaymentEngine debtRepaymentEngine;

    @Mock
    private GoalFeasibilityEngine goalFeasibilityEngine;

    @Mock
    private FinancialValueEngine financialValueEngine;

    @Mock
    private TaxStrategyEngine taxStrategyEngine;

    private MatchCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MatchCalculator(
                financialAssetEngine,
                debtRepaymentEngine,
                goalFeasibilityEngine,
                financialValueEngine,
                taxStrategyEngine
        );
    }

    @Test
    void 전체_금융궁합도를_계산하고_결과를_조립한다() {
        MatchCalculationInput input = createInput();

        when(financialAssetEngine.calculateScore(input))
                .thenReturn(new BigDecimal("15.52"));

        when(financialAssetEngine.calculateCoupleAssetRatio(input))
                .thenReturn(1.45);

        when(debtRepaymentEngine.calculateScore(input))
                .thenReturn(new BigDecimal("14.00"));

        when(debtRepaymentEngine.calculateMemberScore(
                input.getMemberA()
        )).thenReturn(62.5);

        when(debtRepaymentEngine.calculateMemberScore(
                input.getMemberB()
        )).thenReturn(100.0);

        when(financialValueEngine.calculateScore(input))
                .thenReturn(new BigDecimal("16.95"));

        when(goalFeasibilityEngine.calculateScore(input))
                .thenReturn(new BigDecimal("17.05"));

        when(goalFeasibilityEngine.calculateExpectedAsset(input))
                .thenReturn(new BigDecimal("362000000"));

        when(taxStrategyEngine.calculateScore(input))
                .thenReturn(new BigDecimal("8.00"));

        when(taxStrategyEngine.isCalculated(input))
                .thenReturn(true);

        MatchCalculationResult result =
                calculator.calculate(input);

        assertEquals(
                new BigDecimal("15.52"),
                result.getAssetStabilityScore()
        );

        assertEquals(
                1.45,
                result.getCoupleAssetRatio(),
                0.0001
        );

        assertEquals(
                new BigDecimal("14.00"),
                result.getDebtRepaymentScore()
        );

        assertEquals(
                new BigDecimal("62.50"),
                result.getMemberADebtScore()
        );

        assertEquals(
                new BigDecimal("100.00"),
                result.getMemberBDebtScore()
        );

        assertEquals(
                new BigDecimal("16.95"),
                result.getFinancialValueScore()
        );

        assertEquals(
                new BigDecimal("17.05"),
                result.getGoalFeasibilityScore()
        );

        assertEquals(
                new BigDecimal("362000000"),
                result.getExpectedAsset()
        );

        assertEquals(
                new BigDecimal("8.00"),
                result.getTaxStrategyScore()
        );

        assertTrue(result.isTaxStrategyCalculated());

        /*
         * 반올림 후 합산:
         *
         * 15.52 → 16
         * 14.00 → 14
         * 16.95 → 17
         * 17.05 → 17
         * 8.00  → 8
         *
         * 총점 = 72
         */
        assertEquals(
                new BigDecimal("72"),
                result.getTotalScore()
        );

        verify(financialAssetEngine)
                .calculateScore(input);

        verify(financialAssetEngine)
                .calculateCoupleAssetRatio(input);

        verify(debtRepaymentEngine)
                .calculateScore(input);

        verify(debtRepaymentEngine)
                .calculateMemberScore(input.getMemberA());

        verify(debtRepaymentEngine)
                .calculateMemberScore(input.getMemberB());

        verify(financialValueEngine)
                .calculateScore(input);

        verify(goalFeasibilityEngine)
                .calculateScore(input);

        verify(goalFeasibilityEngine)
                .calculateExpectedAsset(input);

        verify(taxStrategyEngine)
                .calculateScore(input);

        verify(taxStrategyEngine)
                .isCalculated(input);
    }

    @Test
    void 절세가_미산출이면_90점을_100점으로_환산한다() {
        MatchCalculationInput input = createInput();

        when(financialAssetEngine.calculateScore(input))
                .thenReturn(new BigDecimal("15.52"));

        when(financialAssetEngine.calculateCoupleAssetRatio(input))
                .thenReturn(1.45);

        when(debtRepaymentEngine.calculateScore(input))
                .thenReturn(new BigDecimal("14.00"));

        when(debtRepaymentEngine.calculateMemberScore(
                input.getMemberA()
        )).thenReturn(62.5);

        when(debtRepaymentEngine.calculateMemberScore(
                input.getMemberB()
        )).thenReturn(100.0);

        when(financialValueEngine.calculateScore(input))
                .thenReturn(new BigDecimal("16.95"));

        when(goalFeasibilityEngine.calculateScore(input))
                .thenReturn(new BigDecimal("17.05"));

        when(goalFeasibilityEngine.calculateExpectedAsset(input))
                .thenReturn(new BigDecimal("362000000"));

        when(taxStrategyEngine.calculateScore(input))
                .thenReturn(new BigDecimal("0.00"));

        when(taxStrategyEngine.isCalculated(input))
                .thenReturn(false);

        MatchCalculationResult result =
                calculator.calculate(input);

        assertEquals(
                new BigDecimal("0.00"),
                result.getTaxStrategyScore()
        );

        assertFalse(result.isTaxStrategyCalculated());

        /*
         * 절세 제외 점수:
         * 16 + 14 + 17 + 17 = 64
         *
         * 90점 만점을 100점으로 환산:
         * 64 × 100 / 90 = 71.11...
         * 정수 반올림 결과 = 71
         */
        assertEquals(
                new BigDecimal("71"),
                result.getTotalScore()
        );
    }

    @Test
    void 전체_궁합계산입력이_null이면_예외를_던진다() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> calculator.calculate(null)
                );

        assertEquals(
                "두 회원의 궁합 계산 입력값이 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 전체_궁합계산의_첫번째_회원입력이_null이면_예외를_던진다() {
        MatchCalculationInput input =
                MatchCalculationInput.builder()
                        .memberA(null)
                        .memberB(
                                MemberCalculationInput.builder()
                                        .build()
                        )
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(input)
        );
    }

    @Test
    void 전체_궁합계산의_두번째_회원입력이_null이면_예외를_던진다() {
        MatchCalculationInput input =
                MatchCalculationInput.builder()
                        .memberA(
                                MemberCalculationInput.builder()
                                        .build()
                        )
                        .memberB(null)
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(input)
        );
    }

    @Test
    void 금융자산점수_계산을_엔진에_위임한다() {
        MatchCalculationInput input = createInput();

        BigDecimal expected =
                new BigDecimal("15.52");

        when(financialAssetEngine.calculateScore(input))
                .thenReturn(expected);

        BigDecimal result =
                calculator.calculateAssetStabilityScore(input);

        assertSame(expected, result);

        verify(financialAssetEngine)
                .calculateScore(input);
    }

    @Test
    void 커플_금융자산비율_계산을_엔진에_위임한다() {
        MatchCalculationInput input = createInput();

        when(financialAssetEngine.calculateCoupleAssetRatio(input))
                .thenReturn(1.45);

        double result =
                calculator.calculateCoupleAssetRatio(input);

        assertEquals(
                1.45,
                result,
                0.0001
        );

        verify(financialAssetEngine)
                .calculateCoupleAssetRatio(input);
    }

    @Test
    void 부채상환점수_계산을_엔진에_위임한다() {
        MatchCalculationInput input = createInput();

        BigDecimal expected =
                new BigDecimal("14.00");

        when(debtRepaymentEngine.calculateScore(input))
                .thenReturn(expected);

        BigDecimal result =
                calculator.calculateDebtRepaymentScore(input);

        assertSame(expected, result);

        verify(debtRepaymentEngine)
                .calculateScore(input);
    }

    @Test
    void 회원_부채점수_계산을_엔진에_위임한다() {
        MemberCalculationInput member =
                MemberCalculationInput.builder()
                        .build();

        when(debtRepaymentEngine.calculateMemberScore(member))
                .thenReturn(70.0);

        double result =
                calculator.calculateMemberDebtScore(member);

        assertEquals(
                70.0,
                result,
                0.0001
        );

        verify(debtRepaymentEngine)
                .calculateMemberScore(member);
    }

    @Test
    void 목표달성가능성점수_계산을_엔진에_위임한다() {
        MatchCalculationInput input = createInput();

        BigDecimal expected =
                new BigDecimal("17.05");

        when(goalFeasibilityEngine.calculateScore(input))
                .thenReturn(expected);

        BigDecimal result =
                calculator.calculateGoalFeasibilityScore(input);

        assertSame(expected, result);

        verify(goalFeasibilityEngine)
                .calculateScore(input);
    }

    @Test
    void 예상자산_계산을_엔진에_위임한다() {
        MatchCalculationInput input = createInput();

        BigDecimal expected =
                new BigDecimal("362000000");

        when(goalFeasibilityEngine.calculateExpectedAsset(input))
                .thenReturn(expected);

        BigDecimal result =
                calculator.calculateExpectedAsset(input);

        assertSame(expected, result);

        verify(goalFeasibilityEngine)
                .calculateExpectedAsset(input);
    }

    @Test
    void 투자가치관점수_계산을_엔진에_위임한다() {
        MatchCalculationInput input = createInput();

        BigDecimal expected =
                new BigDecimal("16.95");

        when(financialValueEngine.calculateScore(input))
                .thenReturn(expected);

        BigDecimal result =
                calculator.calculateFinancialValueScore(input);

        assertSame(expected, result);

        verify(financialValueEngine)
                .calculateScore(input);
    }

    @Test
    void 절세활용도점수_계산을_엔진에_위임한다() {
        MatchCalculationInput input = createInput();

        BigDecimal expected =
                new BigDecimal("8.00");

        when(taxStrategyEngine.calculateScore(input))
                .thenReturn(expected);

        BigDecimal result =
                calculator.calculateTaxStrategyScore(input);

        assertSame(expected, result);

        verify(taxStrategyEngine)
                .calculateScore(input);
    }

    @Test
    void 호환용_절세한도가_엔진의_한도와_같다() {
        assertEquals(
                TaxStrategyEngine.PENSION_SAVING_ANNUAL_LIMIT,
                MatchCalculator.PENSION_SAVING_ANNUAL_LIMIT
        );

        assertEquals(
                TaxStrategyEngine.PENSION_IRP_ANNUAL_LIMIT,
                MatchCalculator.PENSION_IRP_ANNUAL_LIMIT
        );

        assertEquals(
                TaxStrategyEngine.ISA_ANNUAL_LIMIT_AMOUNT,
                MatchCalculator.ISA_ANNUAL_LIMIT_AMOUNT
        );
    }

    private MatchCalculationInput createInput() {
        MemberCalculationInput memberA =
                MemberCalculationInput.builder()
                        .build();

        MemberCalculationInput memberB =
                MemberCalculationInput.builder()
                        .build();

        return MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();
    }
}