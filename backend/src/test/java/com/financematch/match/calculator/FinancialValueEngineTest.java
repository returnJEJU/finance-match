package com.financematch.match.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FinancialValueEngineTest {

    private final FinancialValueEngine engine =
            new FinancialValueEngine();

    @Test
    void 투자_가치관_일치도_점수를_계산한다() {
        MemberCalculationInput memberA = createMember(
                3,
                3,
                3,
                4
        );

        MemberCalculationInput memberB = createMember(
                4,
                4,
                4,
                2
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        /*
         * 금융자산 비중:
         * 차이 1 → 5 * (1 - 1/4) = 3.75
         *
         * 투자 경험:
         * 차이 1 → 4 * (1 - 1/4) = 3.00
         *
         * 금융상품 이해도:
         * 차이 1 → 4 * (1 - 1/4) = 3.00
         *
         * 원금 보존:
         * 차이 2 → 12 * (1 - 2/5) = 7.20
         *
         * 총점 = 16.95
         */
        BigDecimal result = engine.calculateScore(input);

        assertEquals(
                new BigDecimal("16.95"),
                result
        );
    }

    @Test
    void 두_회원의_투자가치관이_완전히_같으면_25점이다() {
        MemberCalculationInput memberA = createMember(
                3,
                3,
                3,
                3
        );

        MemberCalculationInput memberB = createMember(
                3,
                3,
                3,
                3
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("25.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 두_회원의_투자가치관이_최대로_다르면_0점이다() {
        MemberCalculationInput memberA = createMember(
                1,
                1,
                1,
                1
        );

        MemberCalculationInput memberB = createMember(
                5,
                5,
                5,
                6
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
    void 모든_점수가_최솟값이어도_정상적으로_계산한다() {
        MemberCalculationInput memberA = createMember(
                1,
                1,
                1,
                1
        );

        MemberCalculationInput memberB = createMember(
                1,
                1,
                1,
                1
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("25.00"),
                engine.calculateScore(input)
        );
    }

    @Test
    void 모든_점수가_최댓값이어도_정상적으로_계산한다() {
        MemberCalculationInput memberA = createMember(
                5,
                5,
                5,
                6
        );

        MemberCalculationInput memberB = createMember(
                5,
                5,
                5,
                6
        );

        MatchCalculationInput input = createInput(
                memberA,
                memberB
        );

        assertEquals(
                new BigDecimal("25.00"),
                engine.calculateScore(input)
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
                "두 회원의 투자 가치관 계산 입력값이 필요합니다.",
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
    void 금융자산비중_점수가_최솟값보다_작으면_예외를_던진다() {
        MemberCalculationInput memberA = createMember(
                0,
                3,
                3,
                3
        );

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
                "금융자산 비중 점수는 1점 이상 5점 이하여야 합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 금융자산비중_점수가_최댓값보다_크면_예외를_던진다() {
        MemberCalculationInput memberA = createMember(
                6,
                3,
                3,
                3
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
    void 투자경험_점수가_범위를_벗어나면_예외를_던진다() {
        MemberCalculationInput memberA = createMember(
                3,
                0,
                3,
                3
        );

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
                "투자 경험 점수는 1점 이상 5점 이하여야 합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 금융상품이해도_점수가_범위를_벗어나면_예외를_던진다() {
        MemberCalculationInput memberA = createMember(
                3,
                3,
                6,
                3
        );

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
                "금융상품 이해도 점수는 1점 이상 5점 이하여야 합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 원금보존태도_점수가_최솟값보다_작으면_예외를_던진다() {
        MemberCalculationInput memberA = createMember(
                3,
                3,
                3,
                0
        );

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
                "원금 보존 태도 점수는 1점 이상 6점 이하여야 합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 원금보존태도_점수가_최댓값보다_크면_예외를_던진다() {
        MemberCalculationInput memberA = createMember(
                3,
                3,
                3,
                7
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
    void 두번째_회원의_점수가_범위를_벗어나도_예외를_던진다() {
        MemberCalculationInput memberB = createMember(
                3,
                3,
                3,
                7
        );

        MatchCalculationInput input = createInput(
                createValidMember(),
                memberB
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
                3,
                3,
                3,
                3
        );
    }

    private MemberCalculationInput createMember(
            int financialAssetRatioScore,
            int investmentExperienceScore,
            int financialKnowledgeScore,
            int capitalPreservationScore
    ) {
        return MemberCalculationInput.builder()
                .financialAssetRatioScore(
                        financialAssetRatioScore
                )
                .investmentExperienceScore(
                        investmentExperienceScore
                )
                .financialKnowledgeScore(
                        financialKnowledgeScore
                )
                .capitalPreservationScore(
                        capitalPreservationScore
                )
                .build();
    }
}