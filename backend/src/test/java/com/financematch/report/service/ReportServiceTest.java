package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.financematch.exception.ApiException;
import com.financematch.report.dto.GoalProgress;
import com.financematch.report.dto.ReportResponse;
import com.financematch.report.dto.ReportStatusResponse;
import com.financematch.report.dto.detail.ReportDetails;
import com.financematch.report.mapper.ReportMapper;
import com.financematch.report.mapper.ReportRow;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportMapper reportMapper;
    @Mock private GoalProgressFormatter goalProgressFormatter;
    @Mock private ReportDetailService reportDetailService;

    @InjectMocks private ReportService reportService;

    private ReportRow fullRow() {
        ReportRow row = new ReportRow();
        ReflectionTestUtils.setField(row, "memberName", "철수");
        ReflectionTestUtils.setField(row, "partnerName", "영희");
        ReflectionTestUtils.setField(row, "totalScore", new BigDecimal("80.00"));
        ReflectionTestUtils.setField(row, "assetStabilityScore", new BigDecimal("20.00"));
        ReflectionTestUtils.setField(row, "assetStabilityReason", "자산 이유");
        ReflectionTestUtils.setField(row, "debtRepaymentScore", new BigDecimal("15.00"));
        ReflectionTestUtils.setField(row, "debtRepaymentReason", "부채 이유");
        ReflectionTestUtils.setField(row, "financialValueScore", new BigDecimal("20.00"));
        ReflectionTestUtils.setField(row, "financialValueReason", "가치관 이유");
        ReflectionTestUtils.setField(row, "goalFeasibilityScore", new BigDecimal("15.00"));
        ReflectionTestUtils.setField(row, "goalFeasibilityReason", "목표 이유");
        ReflectionTestUtils.setField(row, "expectedAsset", new BigDecimal("120000000"));
        ReflectionTestUtils.setField(row, "taxStrategyScore", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(row, "taxStrategyReason", "절세 이유");
        ReflectionTestUtils.setField(row, "targetAmount", new BigDecimal("100000000"));
        ReflectionTestUtils.setField(row, "targetMonths", 60);
        ReflectionTestUtils.setField(row, "loanPurpose", "HOUSING");
        ReflectionTestUtils.setField(row, "investmentProfileMe", "안정형");
        ReflectionTestUtils.setField(row, "investmentProfileYou", "적극형");
        ReflectionTestUtils.setField(row, "investmentProfileWe", "균형형");
        return row;
    }

    @Test
    void row가_없으면_리포트를_찾을_수_없다는_예외를_던진다() {
        when(reportMapper.findReportRowByMemberId(1L)).thenReturn(null);

        ApiException exception = assertThrows(ApiException.class, () -> reportService.getReport(1L));
        assertEquals("리포트를 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    void 다섯_축_중_하나라도_reason이_비어있으면_준비중_예외를_던진다() {
        ReportRow row = fullRow();
        ReflectionTestUtils.setField(row, "taxStrategyReason", null);
        when(reportMapper.findReportRowByMemberId(1L)).thenReturn(row);

        ApiException exception = assertThrows(ApiException.class, () -> reportService.getReport(1L));
        assertEquals("리포트를 준비하고 있어요.", exception.getMessage());
    }

    @Test
    void 다섯_축이_모두_채워지면_완성된_응답을_반환한다() {
        ReportRow row = fullRow();
        when(reportMapper.findReportRowByMemberId(1L)).thenReturn(row);
        GoalProgress goalProgress = new GoalProgress(true, "+2,000만원", "2,000만원 초과", "1억 2,000만원", "120%");
        when(goalProgressFormatter.format(row.getExpectedAsset(), row.getTargetAmount()))
                .thenReturn(goalProgress);
        ReportDetails details = new ReportDetails(null, null, null, null, null, null);
        when(reportDetailService.getDetails(1L)).thenReturn(details);

        ReportResponse response = reportService.getReport(1L);

        assertEquals("철수", response.getName());
        assertEquals("영희", response.getPartnerName());
        assertEquals(new BigDecimal("80.00"), response.getTotalScore());
        assertEquals(5, response.getScoreAxes().size());
        assertEquals("ASSET_STABILITY", response.getScoreAxes().get(0).getKey());
        assertEquals("TAX_STRATEGY", response.getScoreAxes().get(4).getKey());
        assertEquals(60, response.getTargetMonths());
        assertEquals("HOUSING", response.getLoanPurpose());
        assertEquals("안정형", response.getInvestmentProfile().getMe());
        assertSame(goalProgress, response.getGoalProgress());
        assertSame(details, response.getScoreDetails());
    }

    @Test
    void expectedAsset이나_targetAmount가_없으면_goalProgress는_null이다() {
        ReportRow row = fullRow();
        ReflectionTestUtils.setField(row, "expectedAsset", null);
        when(reportMapper.findReportRowByMemberId(1L)).thenReturn(row);
        when(reportDetailService.getDetails(1L))
                .thenReturn(new ReportDetails(null, null, null, null, null, null));

        ReportResponse response = reportService.getReport(1L);

        assertNull(response.getGoalProgress());
    }

    @Test
    void row가_없으면_상태는_0대5로_미완성이다() {
        when(reportMapper.findReportRowByMemberId(1L)).thenReturn(null);

        ReportStatusResponse status = reportService.getReportStatus(1L);

        assertFalse(status.isReady());
        assertEquals(0, status.getCompletedAxes());
        assertEquals(5, status.getTotalAxes());
    }

    @Test
    void 일부_축만_채워지면_진행중_상태를_반환한다() {
        ReportRow row = fullRow();
        ReflectionTestUtils.setField(row, "taxStrategyReason", null);
        when(reportMapper.findReportRowByMemberId(1L)).thenReturn(row);

        ReportStatusResponse status = reportService.getReportStatus(1L);

        assertFalse(status.isReady());
        assertEquals(4, status.getCompletedAxes());
    }

    @Test
    void 다섯_축이_모두_채워지면_준비완료_상태를_반환한다() {
        ReportRow row = fullRow();
        when(reportMapper.findReportRowByMemberId(1L)).thenReturn(row);

        ReportStatusResponse status = reportService.getReportStatus(1L);

        assertTrue(status.isReady());
        assertEquals(5, status.getCompletedAxes());
    }
}
