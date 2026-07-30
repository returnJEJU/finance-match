package com.financematch.report.service;

import java.math.BigDecimal;

/**
 * 원 단위 금액을 "1억 2000만원" · "8000만원" · "1억" 형태로 변환하는 공용 유틸.
 *
 * <p>만원 단위까지는 반올림 없이 그대로 보여주고, 1만원 미만 소수점만 버린다
 * (예: 8,050만원 → "8050만원", 10.2만원 → "10만원").
 */
public final class WonAmountFormatter {

    private WonAmountFormatter() {}

    public static String format(BigDecimal won) {
        long value = won.longValueExact();
        long eok = value / 100_000_000L;
        long man = (value % 100_000_000L) / 10_000L;

        if (eok > 0 && man > 0) {
            return eok + "억 " + man + "만원";
        }
        if (eok > 0) {
            return eok + "억";
        }
        return man + "만원";
    }
}
