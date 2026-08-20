package com.financematch.match.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class GoalFeasibilityEngineTest {

    private final GoalFeasibilityEngine engine =
            new GoalFeasibilityEngine();

    @Test
    void 일반목표의_달성가능성_점수를_계산한다() {
        /*
         * 합산 금융자산: 100,000,000
         * 연금저축 + IRP 잔액: 30,000,000
         * 현재 사용 가능 자산: 70,000,000
         *
         * 월 가용금액: 1,000,000
         * 연금저축 + IRP 연간 납입액: 12,000,000
         * 월 연금 납입액: 1,000,000
         * 실제 월 가용금액: 0
         *
         * 12개월 후 예상 자산:
         * 70,000,000 * 1.03 = 72,100,000
         *
         * 목표금액: 100,000,000
         * 달성률: 0.721
         * 점수: 20 * 0.721 = 14.42
         */
        MemberCalculationInput memberA = createMember(
                "60000000",
                "500000",
                "10000000",
                "5000000",
                "4000000",
                "2000000"
        );

        MemberCalculationInput memberB = createMember(
                "40000000",
                "500000",
                "10000000",
                "5000000",
                "4000000",
                "2000000"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB,
                "HOUSING",
                "100000000",
                12
        );

        assertEquals(
                new BigDecimal("14.42"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 일반목표의_예상자산을_계산한다() {
        MemberCalculationInput memberA = createMember(
                "60000000",
                "500000",
                "10000000",
                "5000000",
                "4000000",
                "2000000"
        );

        MemberCalculationInput memberB = createMember(
                "40000000",
                "500000",
                "10000000",
                "5000000",
                "4000000",
                "2000000"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB,
                "HOUSING",
                "100000000",
                12
        );

        assertEquals(
                new BigDecimal("72100000"),
                engine.calculateExpectedAsset(input)
        );
    }

    @Test
    void 은퇴목표에서는_연금잔액을_제외하지_않는다() {
        /*
         * 은퇴 목표는 금융자산에서 연금저축·IRP 잔액을 빼지 않는다.
         * 월 가용금액은 0원이므로:
         *
         * 100,000,000 * 1.03 = 103,000,000
         */
        MemberCalculationInput memberA = createMember(
                "60000000",
                "0",
                "10000000",
                "5000000",
                "4000000",
                "2000000"
        );

        MemberCalculationInput memberB = createMember(
                "40000000",
                "0",
                "10000000",
                "5000000",
                "4000000",
                "2000000"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB,
                "RETIREMENT",
                "103000000",
                12
        );

        assertEquals(
                new BigDecimal("103000000"),
                engine.calculateExpectedAsset(input)
        );

        assertEquals(
                new BigDecimal("20.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 예상자산이_목표금액보다_커도_최대점수는_20점이다() {
        MemberCalculationInput memberA = createMember(
                "100000000",
                "0",
                "0",
                "0",
                "0",
                "0"
        );

        MemberCalculationInput memberB = createMember(
                "100000000",
                "0",
                "0",
                "0",
                "0",
                "0"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB,
                "RETIREMENT",
                "100000000",
                12
        );

        assertEquals(
                new BigDecimal("20.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 사용가능한_자산과_월가용금액이_모두_0이면_0점이다() {
        MemberCalculationInput memberA = createMember(
                "0",
                "0",
                "0",
                "0",
                "0",
                "0"
        );

        MemberCalculationInput memberB = createMember(
                "0",
                "0",
                "0",
                "0",
                "0",
                "0"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB,
                "HOUSING",
                "100000000",
                12
        );

        assertEquals(
                new BigDecimal("0.00"),
                engine.calculateScore(input)
        );

        assertEquals(
                new BigDecimal("0"),
                engine.calculateExpectedAsset(input)
        );
    }

    @Test
    void 연금잔액이_금융자산보다_크면_현재가용자산을_0으로_보정한다() {
        MemberCalculationInput memberA = createMember(
                "10000000",
                "0",
                "20000000",
                "10000000",
                "0",
                "0"
        );

        MemberCalculationInput memberB = createMember(
                "10000000",
                "0",
                "20000000",
                "10000000",
                "0",
                "0"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB,
                "HOUSING",
                "100000000",
                12
        );

        assertEquals(
                new BigDecimal("0"),
                engine.calculateExpectedAsset(input)
        );
    }

    @Test
    void 연금납입액이_월가용금액보다_크면_월가용금액을_0으로_보정한다() {
        MemberCalculationInput memberA = createMember(
                "0",
                "100000",
                "0",
                "0",
                "6000000",
                "0"
        );

        MemberCalculationInput memberB = createMember(
                "0",
                "100000",
                "0",
                "0",
                "6000000",
                "0"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB,
                "HOUSING",
                "100000000",
                12
        );

        assertEquals(
                new BigDecimal("0"),
                engine.calculateExpectedAsset(input)
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
                "두 회원의 목표 계산 입력값이 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 첫번째_회원입력이_null이면_예외를_던진다() {
        MatchCalculationInput input =
                MatchCalculationInput.builder()
                        .memberA(null)
                        .memberB(createValidMember())
                        .firstGoalType("HOUSING")
                        .targetAmount(
                                new BigDecimal("100000000")
                        )
                        .targetPeriodMonths(12)
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
                        .firstGoalType("HOUSING")
                        .targetAmount(
                                new BigDecimal("100000000")
                        )
                        .targetPeriodMonths(12)
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateScore(input)
        );
    }

    @Test
    void 목표유형이_null이면_예외를_던진다() {
        MatchCalculationInput input = createInput(
                createValidMember(),
                createValidMember(),
                null,
                "100000000",
                12
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(input)
                );

        assertEquals(
                "목표 유형이 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 목표금액이_null이면_예외를_던진다() {
        MatchCalculationInput input =
                MatchCalculationInput.builder()
                        .memberA(createValidMember())
                        .memberB(createValidMember())
                        .firstGoalType("HOUSING")
                        .targetAmount(null)
                        .targetPeriodMonths(12)
                        .build();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(input)
                );

        assertEquals(
                "목표금액은 0보다 커야 합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 목표금액이_0이면_예외를_던진다() {
        MatchCalculationInput input = createInput(
                createValidMember(),
                createValidMember(),
                "HOUSING",
                "0",
                12
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateScore(input)
        );
    }

    @Test
    void 목표금액이_음수이면_예외를_던진다() {
        MatchCalculationInput input = createInput(
                createValidMember(),
                createValidMember(),
                "HOUSING",
                "-100000000",
                12
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateScore(input)
        );
    }

    @Test
    void 목표기간이_0개월이면_예외를_던진다() {
        MatchCalculationInput input = createInput(
                createValidMember(),
                createValidMember(),
                "HOUSING",
                "100000000",
                0
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(input)
                );

        assertEquals(
                "목표기간은 1개월 이상이어야 합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 목표기간이_음수이면_예외를_던진다() {
        MatchCalculationInput input = createInput(
                createValidMember(),
                createValidMember(),
                "HOUSING",
                "100000000",
                -1
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateScore(input)
        );
    }

    @Test
    void 금융자산이_null이면_예외를_던진다() {
        MemberCalculationInput member =
                MemberCalculationInput.builder()
                        .financialAsset(null)
                        .monthlyAvailableAmount(BigDecimal.ZERO)
                        .pensionSavingBalance(BigDecimal.ZERO)
                        .irpBalance(BigDecimal.ZERO)
                        .pensionAnnualPayment(BigDecimal.ZERO)
                        .irpAnnualPayment(BigDecimal.ZERO)
                        .build();

        assertInvalidMember(member);
    }

    @Test
    void 월가용금액이_null이면_예외를_던진다() {
        MemberCalculationInput member =
                MemberCalculationInput.builder()
                        .financialAsset(BigDecimal.ZERO)
                        .monthlyAvailableAmount(null)
                        .pensionSavingBalance(BigDecimal.ZERO)
                        .irpBalance(BigDecimal.ZERO)
                        .pensionAnnualPayment(BigDecimal.ZERO)
                        .irpAnnualPayment(BigDecimal.ZERO)
                        .build();

        assertInvalidMember(member);
    }

    @Test
    void 연금저축잔액이_null이면_예외를_던진다() {
        MemberCalculationInput member =
                MemberCalculationInput.builder()
                        .financialAsset(BigDecimal.ZERO)
                        .monthlyAvailableAmount(BigDecimal.ZERO)
                        .pensionSavingBalance(null)
                        .irpBalance(BigDecimal.ZERO)
                        .pensionAnnualPayment(BigDecimal.ZERO)
                        .irpAnnualPayment(BigDecimal.ZERO)
                        .build();

        assertInvalidMember(member);
    }

    @Test
    void IRP잔액이_null이면_예외를_던진다() {
        MemberCalculationInput member =
                MemberCalculationInput.builder()
                        .financialAsset(BigDecimal.ZERO)
                        .monthlyAvailableAmount(BigDecimal.ZERO)
                        .pensionSavingBalance(BigDecimal.ZERO)
                        .irpBalance(null)
                        .pensionAnnualPayment(BigDecimal.ZERO)
                        .irpAnnualPayment(BigDecimal.ZERO)
                        .build();

        assertInvalidMember(member);
    }

    @Test
    void 연금저축_연간납입액이_null이면_예외를_던진다() {
        MemberCalculationInput member =
                MemberCalculationInput.builder()
                        .financialAsset(BigDecimal.ZERO)
                        .monthlyAvailableAmount(BigDecimal.ZERO)
                        .pensionSavingBalance(BigDecimal.ZERO)
                        .irpBalance(BigDecimal.ZERO)
                        .pensionAnnualPayment(null)
                        .irpAnnualPayment(BigDecimal.ZERO)
                        .build();

        assertInvalidMember(member);
    }

    @Test
    void IRP_연간납입액이_null이면_예외를_던진다() {
        MemberCalculationInput member =
                MemberCalculationInput.builder()
                        .financialAsset(BigDecimal.ZERO)
                        .monthlyAvailableAmount(BigDecimal.ZERO)
                        .pensionSavingBalance(BigDecimal.ZERO)
                        .irpBalance(BigDecimal.ZERO)
                        .pensionAnnualPayment(BigDecimal.ZERO)
                        .irpAnnualPayment(null)
                        .build();

        assertInvalidMember(member);
    }

    @Test
    void 두번째_회원의_목표입력값이_null이어도_예외를_던진다() {
        MemberCalculationInput invalidMember =
                MemberCalculationInput.builder()
                        .financialAsset(null)
                        .monthlyAvailableAmount(BigDecimal.ZERO)
                        .pensionSavingBalance(BigDecimal.ZERO)
                        .irpBalance(BigDecimal.ZERO)
                        .pensionAnnualPayment(BigDecimal.ZERO)
                        .irpAnnualPayment(BigDecimal.ZERO)
                        .build();

        MatchCalculationInput input = createInput(
                createValidMember(),
                invalidMember,
                "HOUSING",
                "100000000",
                12
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateScore(input)
        );
    }

    private void assertInvalidMember(
            MemberCalculationInput invalidMember
    ) {
        MatchCalculationInput input = createInput(
                invalidMember,
                createValidMember(),
                "HOUSING",
                "100000000",
                12
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(input)
                );

        assertEquals(
                "회원의 목표 계산 입력값이 필요합니다.",
                exception.getMessage()
        );
    }

    private MatchCalculationInput createInput(
            MemberCalculationInput memberA,
            MemberCalculationInput memberB,
            String firstGoalType,
            String targetAmount,
            int targetPeriodMonths
    ) {
        return MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .firstGoalType(firstGoalType)
                .targetAmount(
                        new BigDecimal(targetAmount)
                )
                .targetPeriodMonths(targetPeriodMonths)
                .build();
    }

    private MemberCalculationInput createValidMember() {
        return createMember(
                "50000000",
                "500000",
                "0",
                "0",
                "0",
                "0"
        );
    }

    private MemberCalculationInput createMember(
            String financialAsset,
            String monthlyAvailableAmount,
            String pensionSavingBalance,
            String irpBalance,
            String pensionAnnualPayment,
            String irpAnnualPayment
    ) {
        return MemberCalculationInput.builder()
                .financialAsset(
                        new BigDecimal(financialAsset)
                )
                .monthlyAvailableAmount(
                        new BigDecimal(monthlyAvailableAmount)
                )
                .pensionSavingBalance(
                        new BigDecimal(pensionSavingBalance)
                )
                .irpBalance(
                        new BigDecimal(irpBalance)
                )
                .pensionAnnualPayment(
                        new BigDecimal(pensionAnnualPayment)
                )
                .irpAnnualPayment(
                        new BigDecimal(irpAnnualPayment)
                )
                .build();
    }
}