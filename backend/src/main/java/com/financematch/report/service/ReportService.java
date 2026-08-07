package com.financematch.report.service;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.report.dto.GoalProgress;
import com.financematch.report.dto.InvestmentProfile;
import com.financematch.report.dto.ReportResponse;
import com.financematch.report.dto.ReportStatusResponse;
import com.financematch.report.dto.ScoreAxis;
import com.financematch.report.mapper.ReportMapper;
import com.financematch.report.mapper.ReportRow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    // ReportStatusResponse.totalAxes 와 짝을 이룬다 — 축이 늘면 여기·거기 같이 바뀐다.
    private static final int TOTAL_AXES = 5;

    private final ReportMapper reportMapper;
    private final GoalProgressFormatter goalProgressFormatter;

    public ReportResponse getReport(Long memberId) {
        ReportRow row = reportMapper.findReportRowByMemberId(memberId);

        // 커플 연동 전이거나 궁합 리포트가 아직 계산 전이면 조회되지 않는다.
        if (row == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "리포트를 찾을 수 없습니다.");
        }

        // 5축 reason은 각자 독립된 UPSERT라(ReportWriteMapper) 축마다 끝나는 시점이 다르다 —
        // 특히 LLM을 쓰는 부채·투자가치관·절세 축은 수십 초 걸릴 수 있어, report 행 자체는 이미
        // 생겼어도(첫 축인 goal_feasibility가 만듦) 나머지 축 reason은 한동안 null이다. 응답 DTO의
        // ScoreAxis.reason은 non-null 계약이라, 하나라도 비어있으면 "아직 준비 안 됨"과 똑같이
        // NOT_FOUND로 응답해 프론트가 이미 갖고 있는 "리포트 준비 중" 처리(isNotReady)를 그대로 타게
        // 한다 — 5축이 전부 채워진 순간에만 한 번에 완성된 응답이 나간다.
        if (completedAxisCount(row) < TOTAL_AXES) {
            throw new ApiException(ErrorCode.NOT_FOUND, "리포트를 준비하고 있어요.");
        }

        List<ScoreAxis> scoreAxes =
                List.of(
                        new ScoreAxis(
                                "ASSET_STABILITY",
                                "금융 자산",
                                row.getAssetStabilityScore(),
                                25,
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
                                20,
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

    // 리포트 준비 중 화면이 진행 바를 그릴 수 있도록, 완성된 응답 대신 진행 상황만 가볍게 조회한다.
    // row 자체가 없으면(커플 미연동·계산 시작 전 포함) 0/5로 본다 — 아직 아무 축도 안 끝난 것과
    // 화면에서 구분할 필요가 없다.
    public ReportStatusResponse getReportStatus(Long memberId) {
        ReportRow row = reportMapper.findReportRowByMemberId(memberId);
        int completed = row == null ? 0 : completedAxisCount(row);

        return new ReportStatusResponse(completed >= TOTAL_AXES, completed, TOTAL_AXES);
    }

    private int completedAxisCount(ReportRow row) {
        int count = 0;
        if (row.getAssetStabilityReason() != null) count++;
        if (row.getDebtRepaymentReason() != null) count++;
        if (row.getFinancialValueReason() != null) count++;
        if (row.getGoalFeasibilityReason() != null) count++;
        if (row.getTaxStrategyReason() != null) count++;
        return count;
    }
}
