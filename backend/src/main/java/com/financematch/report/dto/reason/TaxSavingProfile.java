package com.financematch.report.dto.reason;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 한 사람의 ISA·IRP·연금저축 절세 축 입력값 묶음. */
@Getter
@AllArgsConstructor
public class TaxSavingProfile {

    private final TaxAccountInput isa;
    private final TaxAccountInput irp;
    private final TaxAccountInput pension;
}
