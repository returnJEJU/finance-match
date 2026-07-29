package com.financematch.match.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financematch.common.ApiResponse;
import com.financematch.match.domain.CompatibilityResult;
import com.financematch.match.dto.CompatibilityResultResponse;
import com.financematch.match.service.MatchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/matches")
@RequiredArgsConstructor
public class MatchController {

    // JWT 인증 구현 후 로그인 회원 ID로 교체
    private static final Long TEMP_MEMBER_ID = 1L;

    private final MatchService matchService;

    @PostMapping("/compatibility")
    public ApiResponse<CompatibilityResultResponse>
    calculateCompatibility() {

        Long memberId = TEMP_MEMBER_ID;

        CompatibilityResult result =
                matchService.getOrCalculateCompatibilityResult(
                        memberId
                );

        CompatibilityResultResponse response =
                CompatibilityResultResponse.from(result);

        return ApiResponse.ok(response);
    }
}