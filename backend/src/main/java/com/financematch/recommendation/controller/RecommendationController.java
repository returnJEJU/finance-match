package com.financematch.recommendation.controller;

import com.financematch.auth.annotation.LoginMember;
import com.financematch.common.ApiResponse;
import com.financematch.recommendation.dto.RecommendationResponse;
import com.financematch.recommendation.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/members/me/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping
    public ApiResponse<Void> createRecommendation(
            @LoginMember Long memberId) {

        recommendationService.createRecommendation(memberId);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<RecommendationResponse> getRecommendation(
            @LoginMember Long memberId) {

        return ApiResponse.ok(recommendationService.getRecommendation(memberId));
    }
}
