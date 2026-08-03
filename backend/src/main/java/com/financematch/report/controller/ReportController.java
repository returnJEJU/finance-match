package com.financematch.report.controller;

import com.financematch.auth.annotation.LoginMember;
import com.financematch.common.ApiResponse;
import com.financematch.report.dto.ReportResponse;
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
}
