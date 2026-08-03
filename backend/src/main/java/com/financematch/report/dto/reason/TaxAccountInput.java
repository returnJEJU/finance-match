package com.financematch.report.dto.reason;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** ISA·IRP·연금저축 계좌 하나의 절세 축 입력값. 미개설이면 opened=false, 나머지 필드는 null. */
@Getter
@AllArgsConstructor
public class TaxAccountInput {

    private final boolean opened;
    private final BigDecimal contributed;
    private final BigDecimal annualLimit;

    public static TaxAccountInput unopened() {
        return new TaxAccountInput(false, null, null);
    }

    public boolean isUnderLimit() {
        return opened && contributed.compareTo(annualLimit) < 0;
    }
}
