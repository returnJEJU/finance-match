package com.financematch.match.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FinancialAssetEngineTest {

    private final FinancialAssetEngine engine =
            new FinancialAssetEngine();

    @Test
    void 금융자산_안정성_점수를_계산한다() {
        /*
         * 각 회원의 금융자산 / 중앙값 = 1
         * 상대자산 점수 = 100 * (1 - 2^(-1)) = 50
         *
         * 가중 원점수:
         * 0.3 * 50 + 0.3 * 50 + 0.4 * 50 = 50
         *
         * 최종 점수:
         * 50 * 25 / 100 = 12.50
         */
        MemberCalculationInput memberA = createMember(
                "100000000",
                "100000000"
        );

        MemberCalculationInput memberB = createMember(
                "200000000",
                "200000000"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        BigDecimal result = engine.calculateScore(input);

        assertEquals(
                new BigDecimal("12.50"),
                result
        );
    }

    @Test
    void 금융자산이_모두_0원이면_0점이다() {
        MemberCalculationInput memberA = createMember(
                "0",
                "100000000"
        );

        MemberCalculationInput memberB = createMember(
                "0",
                "200000000"
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
    void 금융자산이_중앙값보다_높으면_점수가_상승한다() {
        /*
         * 두 회원 모두 금융자산 / 중앙값 = 2
         * 상대자산 점수 = 100 * (1 - 2^(-2)) = 75
         * 최종 점수 = 75 * 25 / 100 = 18.75
         */
        MemberCalculationInput memberA = createMember(
                "200000000",
                "100000000"
        );

        MemberCalculationInput memberB = createMember(
                "400000000",
                "200000000"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("18.75"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 커플_금융자산_비율을_계산한다() {
        /*
         * 합산 금융자산 = 100,000,000 + 200,000,000
         * 합산 중앙값 = 100,000,000 + 200,000,000
         * 비율 = 1
         */
        MemberCalculationInput memberA = createMember(
                "100000000",
                "100000000"
        );

        MemberCalculationInput memberB = createMember(
                "200000000",
                "200000000"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        double result =
                engine.calculateCoupleAssetRatio(input);

        assertEquals(
                1.0,
                result,
                0.0001
        );
    }

    @Test
    void 서로_다른_중앙값을_합산해_커플자산비율을_계산한다() {
        /*
         * 합산 금융자산 = 50,000,000 + 150,000,000
         *                 = 200,000,000
         *
         * 합산 중앙값 = 100,000,000 + 300,000,000
         *              = 400,000,000
         *
         * 비율 = 0.5
         */
        MemberCalculationInput memberA = createMember(
                "50000000",
                "100000000"
        );

        MemberCalculationInput memberB = createMember(
                "150000000",
                "300000000"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                0.5,
                engine.calculateCoupleAssetRatio(input),
                0.0001
        );
    }

    @Test
    void 음수_금융자산은_0으로_보정한다() {
        MemberCalculationInput memberA = createMember(
                "-10000000",
                "100000000"
        );

        MemberCalculationInput memberB = createMember(
                "-20000000",
                "200000000"
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("0.00"),
                engine.calculateScore(input)
        );

        assertEquals(
                0.0,
                engine.calculateCoupleAssetRatio(input),
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
                "두 회원의 금융자산 계산 입력값이 필요합니다.",
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
    void 커플자산비율_계산입력이_null이면_예외를_던진다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateCoupleAssetRatio(null)
        );
    }

    @Test
    void 금융자산이_null이면_예외를_던진다() {
        MemberCalculationInput memberA =
                MemberCalculationInput.builder()
                        .financialAsset(null)
                        .ageGroupAssetMedian(
                                new BigDecimal("100000000")
                        )
                        .build();

        MatchCalculationInput input = createInput(
                memberA,
                createValidMember()
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(input)
                );

        assertEquals(
                "금융자산이 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 두번째_회원의_금융자산이_null이면_예외를_던진다() {
        MemberCalculationInput memberB =
                MemberCalculationInput.builder()
                        .financialAsset(null)
                        .ageGroupAssetMedian(
                                new BigDecimal("100000000")
                        )
                        .build();

        MatchCalculationInput input = createInput(
                createValidMember(),
                memberB
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateScore(input)
        );
    }

    @Test
    void 금융자산_중앙값이_null이면_예외를_던진다() {
        MemberCalculationInput memberA =
                MemberCalculationInput.builder()
                        .financialAsset(
                                new BigDecimal("100000000")
                        )
                        .ageGroupAssetMedian(null)
                        .build();

        MatchCalculationInput input = createInput(
                memberA,
                createValidMember()
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> engine.calculateScore(input)
                );

        assertEquals(
                "금융자산 중앙값은 0보다 커야 합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 금융자산_중앙값이_0이면_예외를_던진다() {
        MemberCalculationInput memberA = createMember(
                "100000000",
                "0"
        );

        MatchCalculationInput input = createInput(
                memberA,
                createValidMember()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateScore(input)
        );
    }

    @Test
    void 금융자산_중앙값이_음수이면_예외를_던진다() {
        MemberCalculationInput memberA = createMember(
                "100000000",
                "-100000000"
        );

        MatchCalculationInput input = createInput(
                memberA,
                createValidMember()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.calculateScore(input)
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
                "100000000",
                "100000000"
        );
    }

    private MemberCalculationInput createMember(
            String financialAsset,
            String ageGroupAssetMedian
    ) {
        return MemberCalculationInput.builder()
                .financialAsset(
                        new BigDecimal(financialAsset)
                )
                .ageGroupAssetMedian(
                        new BigDecimal(ageGroupAssetMedian)
                )
                .build();
    }
}