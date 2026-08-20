package com.financematch.report.dto.reason;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * 절세 계좌의 한도 미달 판정.
 *
 * <p>리포트 문구가 "한도까지 얼마 남음" 과 "한도를 채움" 중 무엇을 낼지 여기서 갈린다. 미개설 계좌는
 * 납입액·한도가 {@code null} 이므로, <b>개설 여부를 먼저 보지 않으면 NullPointerException</b> 이 난다.
 */
class TaxAccountInputTest {

    private static final BigDecimal LIMIT = new BigDecimal(20_000_000);

    @Test
    void 개설하지_않은_계좌는_한도_미달이_아니다() {
        // 납입액·한도가 null 이라 비교를 시도하면 터진다. 개설 여부에서 먼저 걸러야 한다.
        assertFalse(TaxAccountInput.unopened().isUnderLimit());
    }

    @Test
    void 납입액이_한도보다_적으면_한도_미달이다() {
        TaxAccountInput account = new TaxAccountInput(true, new BigDecimal(12_000_000), LIMIT);

        assertTrue(account.isUnderLimit());
    }

    @Test
    void 납입액이_한도와_같으면_한도_미달이_아니다() {
        // 경계값. "미만" 이 아니라 "이하" 로 잘못 쓰면 한도를 채운 사람에게 더 넣으라고 안내한다.
        TaxAccountInput account = new TaxAccountInput(true, LIMIT, LIMIT);

        assertFalse(account.isUnderLimit());
    }

    @Test
    void 납입액이_한도를_넘어도_한도_미달이_아니다() {
        TaxAccountInput account = new TaxAccountInput(true, new BigDecimal(25_000_000), LIMIT);

        assertFalse(account.isUnderLimit());
    }
}
