package com.financematch.asset.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 마이데이터 도메인 record 의 입력 검증.
 *
 * <p>이 record 들은 <b>외부(마이데이터 연동)에서 들어온 값</b>으로 만들어진다. 우리가 만든 값이 아니라
 * 받아온 값이므로, 생성 시점에 걸러내지 못하면 잘못된 데이터가 그대로 계산 엔진과 DB 까지 흘러간다.
 * 금액이 음수인 채로 자산 합계에 들어가는 식이다.
 *
 * <p>compact 생성자의 검증은 조건이 {@code ||} 로 길게 이어져 있어, 한 가지 경우만 확인하면 나머지
 * 조건은 단축 평가에 가려 한 번도 판정되지 않는다. 그래서 <b>조건마다 그것이 첫 번째로 걸리는 입력</b>을
 * 하나씩 넣는다.
 */
class MyDataRecordValidationTest {

    private static final BigDecimal AMOUNT = new BigDecimal("1000");
    private static final BigDecimal NEGATIVE = new BigDecimal("-1");

    @Nested
    class 자산 {

        @Test
        void 정상_값은_그대로_만들어진다() {
            MyDataAsset asset =
                    new MyDataAsset("국민은행", "정기예금", AssetCategory.BANK_SAVINGS, AMOUNT);

            assertEquals("국민은행", asset.institutionName());
            assertEquals(AMOUNT, asset.balance());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource(
                "com.financematch.asset.domain.MyDataRecordValidationTest#invalidAssetArguments")
        void 잘못된_자산_정보는_거절한다(
                String 사유, String institution, String product, AssetCategory category,
                BigDecimal balance) {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MyDataAsset(institution, product, category, balance));
        }
    }

    static Stream<Arguments> invalidAssetArguments() {
        return Stream.of(
                Arguments.of("기관명 없음", null, "정기예금", AssetCategory.BANK_SAVINGS, AMOUNT),
                Arguments.of("기관명 공백", "  ", "정기예금", AssetCategory.BANK_SAVINGS, AMOUNT),
                Arguments.of("상품명 없음", "국민은행", null, AssetCategory.BANK_SAVINGS, AMOUNT),
                Arguments.of("상품명 공백", "국민은행", "  ", AssetCategory.BANK_SAVINGS, AMOUNT),
                Arguments.of("분류 없음", "국민은행", "정기예금", null, AMOUNT),
                Arguments.of("잔액 없음", "국민은행", "정기예금", AssetCategory.BANK_SAVINGS, null),
                Arguments.of("잔액 음수", "국민은행", "정기예금", AssetCategory.BANK_SAVINGS, NEGATIVE));
    }

    @Nested
    class 대출 {

        @Test
        void 정상_값은_그대로_만들어진다() {
            assertDoesNotThrow(
                    () -> new MyDataLoan("국민은행", "신용대출", AMOUNT, AMOUNT, AMOUNT, true));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource(
                "com.financematch.asset.domain.MyDataRecordValidationTest#invalidLoanArguments")
        void 잘못된_대출_정보는_거절한다(
                String 사유, String institution, String product, BigDecimal balance,
                BigDecimal annualPayment, BigDecimal interestRate) {

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new MyDataLoan(
                                    institution, product, balance, annualPayment, interestRate,
                                    false));
        }
    }

    static Stream<Arguments> invalidLoanArguments() {
        return Stream.of(
                Arguments.of("기관명 없음", null, "신용대출", AMOUNT, AMOUNT, AMOUNT),
                Arguments.of("기관명 공백", " ", "신용대출", AMOUNT, AMOUNT, AMOUNT),
                Arguments.of("상품명 없음", "국민은행", null, AMOUNT, AMOUNT, AMOUNT),
                Arguments.of("상품명 공백", "국민은행", " ", AMOUNT, AMOUNT, AMOUNT),
                Arguments.of("잔액 없음", "국민은행", "신용대출", null, AMOUNT, AMOUNT),
                Arguments.of("잔액 음수", "국민은행", "신용대출", NEGATIVE, AMOUNT, AMOUNT),
                Arguments.of("연상환액 없음", "국민은행", "신용대출", AMOUNT, null, AMOUNT),
                Arguments.of("연상환액 음수", "국민은행", "신용대출", AMOUNT, NEGATIVE, AMOUNT),
                Arguments.of("금리 없음", "국민은행", "신용대출", AMOUNT, AMOUNT, null),
                Arguments.of("금리 음수", "국민은행", "신용대출", AMOUNT, AMOUNT, NEGATIVE));
    }

    @Nested
    class 연금_ISA {

        @Test
        void 정상_값은_그대로_만들어진다() {
            assertDoesNotThrow(MyDataRecordValidationTest::pensionIsa);
        }

        @ParameterizedTest(name = "{0}번째 금액이 없으면 거절")
        @MethodSource(
                "com.financematch.asset.domain.MyDataRecordValidationTest#amountPositions")
        void 금액이_없으면_거절한다(int position) {
            BigDecimal[] amounts = sixAmounts();
            amounts[position] = null;

            assertThrows(IllegalArgumentException.class, () -> pensionIsaWith(amounts));
        }

        @ParameterizedTest(name = "{0}번째 금액이 음수면 거절")
        @MethodSource(
                "com.financematch.asset.domain.MyDataRecordValidationTest#amountPositions")
        void 금액이_음수면_거절한다(int position) {
            BigDecimal[] amounts = sixAmounts();
            amounts[position] = NEGATIVE;

            assertThrows(IllegalArgumentException.class, () -> pensionIsaWith(amounts));
        }

        @Test
        void 절세_평가_대상_여부가_없으면_거절한다() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> pensionIsaWithStatus(null, "ELIGIBLE"));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> pensionIsaWithStatus("ELIGIBLE", null));
        }
    }

    static Stream<Arguments> amountPositions() {
        return Stream.of(
                Arguments.of(0), Arguments.of(1), Arguments.of(2),
                Arguments.of(3), Arguments.of(4), Arguments.of(5));
    }

    @Nested
    class 스냅샷 {

        @Test
        void 목록은_복사해서_보관한다() {
            List<MyDataAsset> assets = new ArrayList<>();
            assets.add(new MyDataAsset("국민은행", "정기예금", AssetCategory.BANK_SAVINGS, AMOUNT));

            MyDataSnapshot snapshot =
                    new MyDataSnapshot(assets, List.of(), pensionIsa(), LocalDateTime.now());

            // 넘긴 목록을 나중에 바꿔도 스냅샷은 영향을 받지 않아야 한다.
            assets.clear();
            assertEquals(1, snapshot.assets().size());
        }

        @Test
        void 자산_목록이_없으면_거절한다() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MyDataSnapshot(null, List.of(), pensionIsa(), LocalDateTime.now()));
        }

        @Test
        void 대출_목록이_없으면_거절한다() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MyDataSnapshot(List.of(), null, pensionIsa(), LocalDateTime.now()));
        }

        @Test
        void 연금_ISA_정보가_없으면_거절한다() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MyDataSnapshot(List.of(), List.of(), null, LocalDateTime.now()));
        }

        @Test
        void 조회_시각이_없으면_거절한다() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MyDataSnapshot(List.of(), List.of(), pensionIsa(), null));
        }
    }

    // ===== 도우미 =====

    private static BigDecimal[] sixAmounts() {
        return new BigDecimal[] {AMOUNT, AMOUNT, AMOUNT, AMOUNT, AMOUNT, AMOUNT};
    }

    private static MyDataPensionIsa pensionIsa() {
        return pensionIsaWith(sixAmounts());
    }

    private static MyDataPensionIsa pensionIsaWith(BigDecimal[] amounts) {
        return new MyDataPensionIsa(
                true, true, false, true,
                amounts[0], amounts[1], amounts[2], amounts[3], amounts[4], amounts[5],
                "ELIGIBLE", "ELIGIBLE");
    }

    private static MyDataPensionIsa pensionIsaWithStatus(String tax, String isa) {
        BigDecimal[] a = sixAmounts();
        return new MyDataPensionIsa(
                true, true, false, true, a[0], a[1], a[2], a[3], a[4], a[5], tax, isa);
    }
}
