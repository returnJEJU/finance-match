package com.financematch.report.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.financematch.report.dto.detail.ReportDetails;

@Getter
@AllArgsConstructor
public class ReportResponse {

    private final String name;
    private final String partnerName;
    private final BigDecimal totalScore;
    private final List<ScoreAxis> scoreAxes;
    private final Integer targetMonths;
    private final String loanPurpose;
    private final InvestmentProfile investmentProfile;
    private final GoalProgress goalProgress;
    private final ReportDetails scoreDetails;
}
