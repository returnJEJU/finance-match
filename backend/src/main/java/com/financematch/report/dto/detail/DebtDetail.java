package com.financematch.report.dto.detail;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DebtDetail {

    private final String summary;
    private final List<DebtGauge> gauges;
}
