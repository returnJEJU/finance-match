package com.financematch.report.controller;

import com.financematch.auth.annotation.LoginMember;
import com.financematch.common.ApiResponse;
import com.financematch.report.dto.ReportResponse;
import com.financematch.report.dto.ReportStatusResponse;
import com.financematch.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/members/me/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("")
    public ApiResponse<ReportResponse> getReport(@LoginMember Long memberId) {

        return ApiResponse.ok(reportService.getReport(memberId));
    }

    // 리포트가 아직 준비 중일 때(NOT_FOUND) 프론트가 진행 바를 그리기 위해 폴링하는 가벼운 엔드포인트.
    @GetMapping("/status")
    public ApiResponse<ReportStatusResponse> getReportStatus(@LoginMember Long memberId) {

        return ApiResponse.ok(reportService.getReportStatus(memberId));
    }
}
