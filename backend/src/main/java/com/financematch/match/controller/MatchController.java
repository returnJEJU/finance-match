package com.financematch.match.controller;

import com.financematch.auth.annotation.LoginMember;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final MatchService matchService;

    @PostMapping("/compatibility")
    public ApiResponse<CompatibilityResultResponse>
    calculateCompatibility(@LoginMember Long memberId) {

        CompatibilityResult result =
                matchService.getOrCalculateCompatibilityResult(
                        memberId
                );

        CompatibilityResultResponse response =
                CompatibilityResultResponse.from(result);

        return ApiResponse.ok(response);
    }

    @GetMapping("/compatibility")
    public ApiResponse<CompatibilityResultResponse>
    getCompatibility(@LoginMember Long memberId) {

        CompatibilityResult result =
                matchService.getCompatibilityResult(memberId);

        CompatibilityResultResponse response =
                CompatibilityResultResponse.from(result);

        return ApiResponse.ok(response);
    }
}