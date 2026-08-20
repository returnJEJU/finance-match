package com.financematch.match.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class TaxStrategyEngineTest {

    private final TaxStrategyEngine engine =
            new TaxStrategyEngine();

    @Test
    void 연금과_ISA_활용도_점수를_계산한다() {
        /*
         * 두 회원의 연금 활용도: 모두 100%
         * 연금 점수: 6점
         *
         * A ISA 활용도: 50%
         * B ISA 활용도: 100%
         * 커플 평균: 75%
         * ISA 점수: 4 * 0.75 = 3점
         *
         * 최종 점수: 9점
         */
        MemberCalculationInput memberA = createMember(
                "6000000",
                "3000000",
                "0",
                "10000000",
                "ELIGIBLE",
                "ELIGIBLE"
        );

        MemberCalculationInput memberB = createMember(
                "6000000",
                "3000000",
                "0",
                "20000000",
                "ELIGIBLE",
                "ELIGIBLE"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("9.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 연금만_평가대상이면_연금점수를_10점으로_환산한다() {
        /*
         * 연금 인정 납입액: 4,500,000
         * 연금 활용률: 50%
         * 연금 원점수: 6 * 0.5 = 3
         *
         * 연금만 평가하므로:
         * 10 * (3 / 6) = 5점
         */
        MemberCalculationInput memberA = createMember(
                "4500000",
                "0",
                "0",
                "0",
                "ELIGIBLE",
                "INELIGIBLE"
        );

        MemberCalculationInput memberB = createMember(
                "4500000",
                "0",
                "0",
                "0",
                "ELIGIBLE",
                "INELIGIBLE"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("5.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void ISA만_평가대상이면_ISA점수를_10점으로_환산한다() {
        /*
         * ISA 납입액 10,000,000 / 한도 20,000,000
         * 활용률 50%
         * ISA 원점수: 4 * 0.5 = 2
         *
         * ISA만 평가하므로:
         * 10 * (2 / 4) = 5점
         */
        MemberCalculationInput memberA = createMember(
                "0",
                "0",
                "0",
                "10000000",
                "INELIGIBLE",
                "ELIGIBLE"
        );

        MemberCalculationInput memberB = createMember(
                "0",
                "0",
                "0",
                "10000000",
                "INELIGIBLE",
                "ELIGIBLE"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("5.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 연금과_ISA가_모두_평가대상이_아니면_0점이다() {
        MemberCalculationInput memberA =
                createIneligibleMember();

        MemberCalculationInput memberB =
                createIneligibleMember();

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("0.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 평가대상이지만_납입액이_모두_0이면_0점이다() {
        MemberCalculationInput memberA = createMember(
                "0",
                "0",
                "0",
                "0",
                "ELIGIBLE",
                "ELIGIBLE"
        );

        MemberCalculationInput memberB = createMember(
                "0",
                "0",
                "0",
                "0",
                "ELIGIBLE",
                "ELIGIBLE"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("0.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 한_회원만_평가대상이면_해당회원만_평균에_포함한다() {
        MemberCalculationInput memberA = createMember(
                "4500000",
                "0",
                "0",
                "10000000",
                "ELIGIBLE",
                "ELIGIBLE"
        );

        MemberCalculationInput memberB = createMember(
                "0",
                "0",
                "0",
                "0",
                "INELIGIBLE",
                "INELIGIBLE"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        /*
         * 평가 대상인 A의 연금과 ISA 활용률이 모두 50%이므로
         * 최종 점수도 5점이다.
         */
        assertEquals(
                new BigDecimal("5.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 납입액이_한도를_초과하면_최대_10점이다() {
        MemberCalculationInput memberA = createMember(
                "10000000",
                "10000000",
                "10000000",
                "30000000",
                "ELIGIBLE",
                "ELIGIBLE"
        );

        MemberCalculationInput memberB = createMember(
                "10000000",
                "10000000",
                "10000000",
                "30000000",
                "ELIGIBLE",
                "ELIGIBLE"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("10.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 연금저축은_600만원까지만_인정한다() {
        MemberCalculationInput memberA = createMember(
                "10000000",
                "0",
                "0",
                "0",
                "ELIGIBLE",
                "INELIGIBLE"
        );

        MemberCalculationInput memberB = createMember(
                "10000000",
                "0",
                "0",
                "0",
                "ELIGIBLE",
                "INELIGIBLE"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        /*
         * 연금저축은 10,000,000원을 납입했어도
         * 최대 6,000,000원만 인정된다.
         *
         * 활용률: 6,000,000 / 9,000,000 = 2/3
         * 연금만 평가하므로 최종 점수도 10 * 2/3 = 6.67
         */
        assertEquals(
                new BigDecimal("6.67"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 연금저축_IRP_DC의_합계는_900만원까지만_인정한다() {
        MemberCalculationInput memberA = createMember(
                "6000000",
                "3000000",
                "1000000",
                "0",
                "ELIGIBLE",
                "INELIGIBLE"
        );

        MemberCalculationInput memberB = createMember(
                "6000000",
                "3000000",
                "1000000",
                "0",
                "ELIGIBLE",
                "INELIGIBLE"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("10.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 첫번째_회원의_연금이_평가대상이면_산출대상이다() {
        MatchCalculationInput input = createInput(
                createMember(
                        "0",
                        "0",
                        "0",
                        "0",
                        "ELIGIBLE",
                        "INELIGIBLE"
                ),
                createIneligibleMember()
        );

        assertTrue(engine.isCalculated(input));
    }

    @Test
    void 두번째_회원의_연금이_평가대상이면_산출대상이다() {
        MatchCalculationInput input = createInput(
                createIneligibleMember(),
                createMember(
                        "0",
                        "0",
                        "0",
                        "0",
                        "ELIGIBLE",
                        "INELIGIBLE"
                )
        );

        assertTrue(engine.isCalculated(input));
    }

    @Test
    void 첫번째_회원의_ISA가_평가대상이면_산출대상이다() {
        MatchCalculationInput input = createInput(
                createMember(
                        "0",
                        "0",
                        "0",
                        "0",
                        "INELIGIBLE",
                        "ELIGIBLE"
                ),
                createIneligibleMember()
        );

        assertTrue(engine.isCalculated(input));
    }

    @Test
    void 두번째_회원의_ISA가_평가대상이면_산출대상이다() {
        MatchCalculationInput input = createInput(
                createIneligibleMember(),
                createMember(
                        "0",
                        "0",
                        "0",
                        "0",
                        "INELIGIBLE",
                        "ELIGIBLE"
                )
        );

        assertTrue(engine.isCalculated(input));
    }

    @Test
    void 모든_절세항목이_평가대상이_아니면_미산출이다() {
        MatchCalculationInput input = createInput(
                createIneligibleMember(),
                createIneligibleMember()
        );

        assertFalse(engine.isCalculated(input));
    }

    @Test
    void 전체입력이_null이면_예외를_던진다() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(null)
                );

        assertEquals(
                "두 회원의 절세 계산 입력값이 필요합니다.",
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
    void isCalculated의_입력이_null이면_예외를_던진다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> engine.isCalculated(null)
        );
    }

    @Test
    void 절세입력값이_null이면_예외를_던진다() {
        assertInvalidMember(
                createMember(
                        null,
                        "0",
                        "0",
                        "0",
                        "ELIGIBLE",
                        "ELIGIBLE"
                )
        );

        assertInvalidMember(
                createMember(
                        "0",
                        null,
                        "0",
                        "0",
                        "ELIGIBLE",
                        "ELIGIBLE"
                )
        );

        assertInvalidMember(
                createMember(
                        "0",
                        "0",
                        null,
                        "0",
                        "ELIGIBLE",
                        "ELIGIBLE"
                )
        );

        assertInvalidMember(
                createMember(
                        "0",
                        "0",
                        "0",
                        null,
                        "ELIGIBLE",
                        "ELIGIBLE"
                )
        );

        assertInvalidMember(
                createMember(
                        "0",
                        "0",
                        "0",
                        "0",
                        null,
                        "ELIGIBLE"
                )
        );

        assertInvalidMember(
                createMember(
                        "0",
                        "0",
                        "0",
                        "0",
                        "ELIGIBLE",
                        null
                )
        );
    }

    @Test
    void 두번째_회원의_절세입력값이_null이어도_예외를_던진다() {
        MemberCalculationInput invalidMember =
                createMember(
                        null,
                        "0",
                        "0",
                        "0",
                        "ELIGIBLE",
                        "ELIGIBLE"
                );

        MatchCalculationInput input = createInput(
                createValidMember(),
                invalidMember
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateScore(input)
        );
    }

    @Test
    void 절세납입액이_음수이면_예외를_던진다() {
        assertNegativePayment(
                createMember(
                        "-1",
                        "0",
                        "0",
                        "0",
                        "ELIGIBLE",
                        "ELIGIBLE"
                )
        );

        assertNegativePayment(
                createMember(
                        "0",
                        "-1",
                        "0",
                        "0",
                        "ELIGIBLE",
                        "ELIGIBLE"
                )
        );

        assertNegativePayment(
                createMember(
                        "0",
                        "0",
                        "-1",
                        "0",
                        "ELIGIBLE",
                        "ELIGIBLE"
                )
        );

        assertNegativePayment(
                createMember(
                        "0",
                        "0",
                        "0",
                        "-1",
                        "ELIGIBLE",
                        "ELIGIBLE"
                )
        );
    }

    @Test
    void 연금평가상태가_알수없는_값이면_예외를_던진다() {
        MemberCalculationInput memberA = createMember(
                "0",
                "0",
                "0",
                "0",
                "UNKNOWN",
                "INELIGIBLE"
        );

        MatchCalculationInput input = createInput(
                memberA,
                createIneligibleMember()
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(input)
                );

        assertEquals(
                "연금 평가 대상 여부를 확인할 수 없습니다.",
                exception.getMessage()
        );
    }

    @Test
    void ISA평가상태가_알수없는_값이면_예외를_던진다() {
        MemberCalculationInput memberA = createMember(
                "0",
                "0",
                "0",
                "0",
                "INELIGIBLE",
                "UNKNOWN"
        );

        MatchCalculationInput input = createInput(
                memberA,
                createIneligibleMember()
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(input)
                );

        assertEquals(
                "ISA 평가 대상 여부를 확인할 수 없습니다.",
                exception.getMessage()
        );
    }

    private void assertInvalidMember(
            MemberCalculationInput invalidMember
    ) {
        MatchCalculationInput input = createInput(
                invalidMember,
                createValidMember()
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(input)
                );

        assertEquals(
                "회원의 절세 계산 입력값이 필요합니다.",
                exception.getMessage()
        );
    }

    private void assertNegativePayment(
            MemberCalculationInput invalidMember
    ) {
        MatchCalculationInput input = createInput(
                invalidMember,
                createValidMember()
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(input)
                );

        assertEquals(
                "절세 납입액은 0보다 작을 수 없습니다.",
                exception.getMessage()
        );
    }

    private MatchCalculationInput createInput(
            MemberCalculationInput memberA,
            MemberCalculationInput memberB
    ) {
        return MatchCalculationInput.builder()
                .memberA(memberA)
                .memberB(memberB)
                .build();
    }

    private MemberCalculationInput createValidMember() {
        return createMember(
                "6000000",
                "3000000",
                "0",
                "20000000",
                "ELIGIBLE",
                "ELIGIBLE"
        );
    }

    private MemberCalculationInput createIneligibleMember() {
        return createMember(
                "0",
                "0",
                "0",
                "0",
                "INELIGIBLE",
                "INELIGIBLE"
        );
    }

    private MemberCalculationInput createMember(
            String pensionAnnualPayment,
            String irpAnnualPayment,
            String dcAnnualPayment,
            String isaAnnualDeposit,
            String taxEligibilityStatus,
            String isaEligibilityStatus
    ) {
        return MemberCalculationInput.builder()
                .pensionAnnualPayment(
                        decimal(pensionAnnualPayment)
                )
                .irpAnnualPayment(
                        decimal(irpAnnualPayment)
                )
                .dcAnnualPayment(
                        decimal(dcAnnualPayment)
                )
                .isaAnnualDeposit(
                        decimal(isaAnnualDeposit)
                )
                .taxEligibilityStatus(
                        taxEligibilityStatus
                )
                .isaEligibilityStatus(
                        isaEligibilityStatus
                )
                .build();
    }

    private BigDecimal decimal(String value) {
        if (value == null) {
            return null;
        }

        return new BigDecimal(value);
    }
}