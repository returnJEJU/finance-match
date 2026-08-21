package com.financematch.asset.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.asset.domain.AssetCategory;
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
 * 자산 연동 응답 record 의 입력 검증.
 *
 * <p>응답 DTO 이지만 값을 그냥 담기만 하지 않고 생성 시점에 검사한다. 잘못된 값이 여기를 통과하면
 * <b>프론트 화면에 음수 자산이 그려지는</b> 식으로 드러나고, 그때는 원인이 어디인지 찾기 어렵다.
 *
 * <p>검증 조건이 {@code ||} 로 이어져 있어 한 경우만 확인하면 나머지는 단축 평가에 가린다. 조건마다
 * 그것이 첫 번째로 걸리는 입력을 하나씩 넣는다.
 */
class AssetResponseValidationTest {

    private static final BigDecimal AMOUNT = new BigDecimal("1000");
    private static final BigDecimal NEGATIVE = new BigDecimal("-1");
    private static final AssetAccountResponse ACCOUNTS =
            new AssetAccountResponse(true, true, false);

    @Nested
    class 자산_요약 {

        @Test
        void 정상_값은_그대로_만들어진다() {
            AssetSummaryResponse summary =
                    new AssetSummaryResponse(AssetCategory.SECURITIES, AMOUNT);

            assertEquals(AssetCategory.SECURITIES, summary.category());
            assertEquals(AMOUNT, summary.amount());
        }

        @Test
        void 분류가_없으면_거절한다() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new AssetSummaryResponse(null, AMOUNT));
        }

        @Test
        void 금액이_없으면_거절한다() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new AssetSummaryResponse(AssetCategory.SECURITIES, null));
        }

        @Test
        void 금액이_음수면_거절한다() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new AssetSummaryResponse(AssetCategory.SECURITIES, NEGATIVE));
        }
    }

    @Nested
    class 연동_응답 {

        @Test
        void 요약_목록은_복사해서_보관한다() {
            List<AssetSummaryResponse> summary = new ArrayList<>();
            summary.add(new AssetSummaryResponse(AssetCategory.BANK_SAVINGS, AMOUNT));

            AssetLinkResponse response =
                    new AssetLinkResponse(
                            AMOUNT, AMOUNT, 1, summary, ACCOUNTS, LocalDateTime.now());

            // 넘긴 목록을 나중에 바꿔도 응답은 영향을 받지 않아야 한다.
            summary.clear();
            assertEquals(1, response.summary().size());
        }

        @Test
        void 요약_목록이_없으면_거절한다() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new AssetLinkResponse(
                                    AMOUNT, AMOUNT, 1, null, ACCOUNTS, LocalDateTime.now()));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource(
                "com.financematch.asset.dto.AssetResponseValidationTest#invalidLinkArguments")
        void 잘못된_연동_응답은_거절한다(
                String 사유, BigDecimal totalAsset, BigDecimal totalDebt, int assetCount,
                AssetAccountResponse accounts, LocalDateTime linkedAt) {

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new AssetLinkResponse(
                                    totalAsset, totalDebt, assetCount, List.of(), accounts,
                                    linkedAt));
        }
    }

    static Stream<Arguments> invalidLinkArguments() {
        LocalDateTime now = LocalDateTime.now();
        return Stream.of(
                Arguments.of("총자산 없음", null, AMOUNT, 1, ACCOUNTS, now),
                Arguments.of("총자산 음수", NEGATIVE, AMOUNT, 1, ACCOUNTS, now),
                Arguments.of("총부채 없음", AMOUNT, null, 1, ACCOUNTS, now),
                Arguments.of("총부채 음수", AMOUNT, NEGATIVE, 1, ACCOUNTS, now),
                Arguments.of("자산 건수 음수", AMOUNT, AMOUNT, -1, ACCOUNTS, now),
                Arguments.of("계좌 정보 없음", AMOUNT, AMOUNT, 1, null, now),
                Arguments.of("연동 시각 없음", AMOUNT, AMOUNT, 1, ACCOUNTS, null));
    }
}
