package com.financematch.report.dto.detail;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvestmentValueRow {

    private final String label;
    private final String me;
    private final String partner;
    private final String match;
}
