package com.financematch.match.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DebtRepaymentEngineTest {

    private final DebtRepaymentEngine engine =
            new DebtRepaymentEngine();

    @Test
    void 두_회원의_부채상환점수를_계산한다() {
        MemberCalculationInput memberA = createMember(
                "20000000",
                "6000000",
                "45000000",
                "90000000"
        );

        MemberCalculationInput memberB = createMember(
                "40000000",
                "10000000",
                "75000000",
                "180000000"
        );

        MatchCalculationInput input =
                MatchCalculationInput.builder()
                        .memberA(memberA)
                        .memberB(memberB)
                        .build();

        BigDecimal result = engine.calculateScore(input);

        assertEquals(
                new BigDecimal("14.00"),
                result
        );
    }

    @Test
    void 두_회원에게_부채가_없으면_20점이다() {
        MemberCalculationInput memberA = createMember(
                "0",
                "0",
                "45000000",
                "90000000"
        );

        MemberCalculationInput memberB = createMember(
                "0",
                "0",
                "75000000",
                "180000000"
        );

        MatchCalculationInput input =
                MatchCalculationInput.builder()
                        .memberA(memberA)
                        .memberB(memberB)
                        .build();

        assertEquals(
                new BigDecimal("20.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 회원의_부채상환_원점수를_계산한다() {
        MemberCalculationInput member = createMember(
                "20000000",
                "6000000",
                "45000000",
                "90000000"
        );

        double result = engine.calculateMemberScore(member);

        assertEquals(
                70.0,
                result,
                0.0001
        );
    }

    @Test
    void 부채가_없으면_회원점수는_100점이다() {
        MemberCalculationInput member = createMember(
                "0",
                "0",
                "0",
                "0"
        );

        assertEquals(
                100.0,
                engine.calculateMemberScore(member),
                0.0001
        );
    }

    @Test
    void 부채는_있지만_소득과_금융자산이_없으면_0점이다() {
        MemberCalculationInput member = createMember(
                "10000000",
                "1000000",
                "0",
                "0"
        );

        assertEquals(
                0.0,
                engine.calculateMemberScore(member),
                0.0001
        );
    }

    @Test
    void 소득은_없지만_금융자산이_있으면_자산비율을_반영한다() {
        MemberCalculationInput member = createMember(
                "50000000",
                "5000000",
                "0",
                "100000000"
        );

        /*
         * 소득이 없으므로 DSR 위험도는 1
         * 부채비율은 50,000,000 / 100,000,000 = 0.5
         *
         * risk = 0.7 * 1 + 0.3 * 0.5 = 0.85
         * score = 100 * (1 - 0.85) = 15
         */
        assertEquals(
                15.0,
                engine.calculateMemberScore(member),
                0.0001
        );
    }

    @Test
    void 금융자산은_없지만_소득이_있으면_DSR을_반영한다() {
        MemberCalculationInput member = createMember(
                "10000000",
                "4000000",
                "50000000",
                "0"
        );

        /*
         * DSR 정규화 값:
         * 4,000,000 / (0.4 * 50,000,000) = 0.2
         *
         * 금융자산이 없으므로 부채비율 위험도는 1
         *
         * risk = 0.7 * 0.2 + 0.3 * 1 = 0.44
         * score = 100 * (1 - 0.44) = 56
         */
        assertEquals(
                56.0,
                engine.calculateMemberScore(member),
                0.0001
        );
    }

    @Test
    void DSR과_부채비율이_한도를_넘으면_회원점수는_0점이다() {
        MemberCalculationInput member = createMember(
                "200000000",
                "50000000",
                "30000000",
                "100000000"
        );

        assertEquals(
                0.0,
                engine.calculateMemberScore(member),
                0.0001
        );
    }

    @Test
    void 전체입력이_null이면_예외를_던진다() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(null)
                );

        assertEquals(
                "두 회원의 부채 계산 입력값이 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 첫번째_회원입력이_null이면_예외를_던진다() {
        MatchCalculationInput input =
                MatchCalculationInput.builder()
                        .memberA(null)
                        .memberB(createValidMember())
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateScore(input)
        );
    }

    @Test
    void 두번째_회원입력이_null이면_예외를_던진다() {
        MatchCalculationInput input =
                MatchCalculationInput.builder()
                        .memberA(createValidMember())
                        .memberB(null)
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateScore(input)
        );
    }

    @Test
    void 회원입력이_null이면_회원점수계산에서_예외를_던진다() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateMemberScore(null)
                );

        assertEquals(
                "회원의 부채 계산 입력값이 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 총부채가_null이면_예외를_던진다() {
        MemberCalculationInput member =
                MemberCalculationInput.builder()
                        .totalDebt(null)
                        .annualDebtPayment(new BigDecimal("6000000"))
                        .annualIncome(new BigDecimal("45000000"))
                        .financialAsset(new BigDecimal("90000000"))
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateMemberScore(member)
        );
    }

    @Test
    void 연간부채상환액이_null이면_예외를_던진다() {
        MemberCalculationInput member =
                MemberCalculationInput.builder()
                        .totalDebt(new BigDecimal("20000000"))
                        .annualDebtPayment(null)
                        .annualIncome(new BigDecimal("45000000"))
                        .financialAsset(new BigDecimal("90000000"))
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateMemberScore(member)
        );
    }

    @Test
    void 연소득이_null이면_예외를_던진다() {
        MemberCalculationInput member =
                MemberCalculationInput.builder()
                        .totalDebt(new BigDecimal("20000000"))
                        .annualDebtPayment(new BigDecimal("6000000"))
                        .annualIncome(null)
                        .financialAsset(new BigDecimal("90000000"))
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateMemberScore(member)
        );
    }

    @Test
    void 금융자산이_null이면_예외를_던진다() {
        MemberCalculationInput member =
                MemberCalculationInput.builder()
                        .totalDebt(new BigDecimal("20000000"))
                        .annualDebtPayment(new BigDecimal("6000000"))
                        .annualIncome(new BigDecimal("45000000"))
                        .financialAsset(null)
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateMemberScore(member)
        );
    }

    @Test
    void 음수_부채는_0으로_보정한다() {
        MemberCalculationInput member = createMember(
                "-10000000",
                "1000000",
                "45000000",
                "90000000"
        );

        assertEquals(
                100.0,
                engine.calculateMemberScore(member),
                0.0001
        );
    }

    private MemberCalculationInput createValidMember() {
        return createMember(
                "20000000",
                "6000000",
                "45000000",
                "90000000"
        );
    }

    private MemberCalculationInput createMember(
            String totalDebt,
            String annualDebtPayment,
            String annualIncome,
            String financialAsset
    ) {
        return MemberCalculationInput.builder()
                .totalDebt(new BigDecimal(totalDebt))
                .annualDebtPayment(new BigDecimal(annualDebtPayment))
                .annualIncome(new BigDecimal(annualIncome))
                .financialAsset(new BigDecimal(financialAsset))
                .build();
    }
}