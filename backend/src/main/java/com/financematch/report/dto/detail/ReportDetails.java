package com.financematch.report.dto.detail;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportDetails {

    private final AssetDetail asset;
    private final InvestmentValueDetail investmentValue;
    private final DebtDetail debt;
    private final GoalDetail goal;
    private final TaxDetail tax;
    private final AiCommentDetail aiComment;
}
