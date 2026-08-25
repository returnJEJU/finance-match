package com.financematch.report.controller;

import com.financematch.auth.annotation.LoginMember;
import com.financematch.common.ApiResponse;
import com.financematch.match.service.MatchService;
import com.financematch.report.dto.ReportResponse;
import com.financematch.report.dto.ReportStatusResponse;
import com.financematch.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/members/me/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final MatchService matchService;

    @GetMapping("")
    public ApiResponse<ReportResponse> getReport(@LoginMember Long memberId) {

        return ApiResponse.ok(reportService.getReport(memberId));
    }

    // 리포트가 아직 준비 중일 때(NOT_FOUND) 프론트가 진행 바를 그리기 위해 폴링하는 가벼운 엔드포인트.
    @GetMapping("/status")
    public ApiResponse<ReportStatusResponse> getReportStatus(@LoginMember Long memberId) {

        return ApiResponse.ok(reportService.getReportStatus(memberId));
    }

    // AI 종합 코멘트 생성이 재시도까지 전부 실패해 비어있을 때, 프론트 "다시 시도" 버튼이 부른다.
    // 실제 생성은 @Async라 이 응답은 즉시 돌아가고, 완성 여부는 이후 GET("")으로 다시 확인해야 한다.
    @PostMapping("/expert-comment/retry")
    public ApiResponse<Void> retryExpertComment(@LoginMember Long memberId) {
        matchService.retryOverallComment(memberId);
        return ApiResponse.ok();
    }
}
