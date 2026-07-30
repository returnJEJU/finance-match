package com.financematch.couple.controller;

import com.financematch.auth.annotation.LoginMember;
import com.financematch.common.ApiResponse;
import com.financematch.couple.dto.CoupleProfileMessageRequest;
import com.financematch.couple.dto.CoupleProfileMessageResponse;
import com.financematch.couple.dto.CreateCoupleRequest;
import com.financematch.couple.dto.CreateCoupleResponse;
import com.financematch.couple.service.CoupleService;
import java.net.URI;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/members/me/couple")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    private static final URI COUPLE_LOCATION = URI.create("/api/v1/members/me/couple");

    @PostMapping("")
    public ResponseEntity<ApiResponse<CreateCoupleResponse>> createCouple(
            @LoginMember Long memberId,
            @Valid @RequestBody CreateCoupleRequest request) {

        CreateCoupleResponse response = coupleService.createCouple(memberId, request);

        return ResponseEntity.created(COUPLE_LOCATION)
                .body(ApiResponse.ok(response));
    }

    @GetMapping("/profile-message")
    public ApiResponse<CoupleProfileMessageResponse> getProfileMessage(
            @LoginMember Long memberId) {
        return ApiResponse.ok(coupleService.getProfileMessage(memberId));
    }

    @PatchMapping("/profile-message")
    public ApiResponse<CoupleProfileMessageResponse> updateProfileMessage(
            @LoginMember Long memberId,
            @Valid @RequestBody CoupleProfileMessageRequest request) {
        return ApiResponse.ok(
                coupleService.updateProfileMessage(
                        memberId,
                        request.getProfileMessage()));
    }
}
