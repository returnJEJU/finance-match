package com.financematch.report.dto.detail;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvestmentValueDetail {

    private final List<String> columns;
    private final List<InvestmentValueRow> rows;
}
