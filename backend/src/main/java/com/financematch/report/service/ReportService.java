package com.financematch.report.service;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.report.dto.GoalProgress;
import com.financematch.report.dto.InvestmentProfile;
import com.financematch.report.dto.ReportResponse;
import com.financematch.report.dto.ScoreAxis;
import com.financematch.report.mapper.ReportMapper;
import com.financematch.report.mapper.ReportRow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final GoalProgressFormatter goalProgressFormatter;

    public ReportResponse getReport(Long memberId) {
        ReportRow row = reportMapper.findReportRowByMemberId(memberId);

        // 커플 연동 전이거나 궁합 리포트가 아직 계산 전이면 조회되지 않는다.
        if (row == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "리포트를 찾을 수 없습니다.");
        }

        List<ScoreAxis> scoreAxes =
                List.of(
                        new ScoreAxis(
                                "ASSET_STABILITY",
                                "금융 자산",
                                row.getAssetStabilityScore(),
                                30,
                                row.getAssetStabilityReason()),
                        new ScoreAxis(
                                "DEBT_REPAYMENT",
                                "부채",
                                row.getDebtRepaymentScore(),
                                20,
                                row.getDebtRepaymentReason()),
                        new ScoreAxis(
                                "FINANCIAL_VALUE",
                                "투자 가치관 일치도",
                                row.getFinancialValueScore(),
                                25,
                                row.getFinancialValueReason()),
                        new ScoreAxis(
                                "GOAL_FEASIBILITY",
                                "목표 달성 가능성",
                                row.getGoalFeasibilityScore(),
                                15,
                                row.getGoalFeasibilityReason()),
                        new ScoreAxis(
                                "TAX_STRATEGY",
                                "절세 활용도",
                                row.getTaxStrategyScore(),
                                10,
                                row.getTaxStrategyReason()));

        InvestmentProfile investmentProfile =
                new InvestmentProfile(
                        row.getInvestmentProfileMe(),
                        row.getInvestmentProfileYou(),
                        row.getInvestmentProfileWe());

        // expected_asset 은 report 컬럼 추가(V20260801_0900) 이전 행에는 없을 수 있어 null-safe 하게 처리한다.
        GoalProgress goalProgress =
                row.getExpectedAsset() == null || row.getTargetAmount() == null
                        ? null
                        : goalProgressFormatter.format(row.getExpectedAsset(), row.getTargetAmount());

        return new ReportResponse(
                row.getMemberName(),
                row.getPartnerName(),
                row.getTotalScore(),
                scoreAxes,
                row.getTargetMonths(),
                row.getLoanPurpose(),
                investmentProfile,
                goalProgress);
    }
}
