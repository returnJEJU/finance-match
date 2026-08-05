package com.financematch.asset.controller;

import com.financematch.asset.dto.AssetLinkResponse;
import com.financematch.asset.service.AssetService;
import com.financematch.auth.annotation.LoginMember;
import com.financematch.common.ApiResponse;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/members/me/assets")
@RequiredArgsConstructor
public class AssetController {

    private static final URI ASSET_LOCATION = URI.create("/api/v1/members/me/assets");

    private final AssetService assetService;

    @GetMapping
    public ApiResponse<AssetLinkResponse> getAssets(
            @LoginMember Long memberId) {
        return ApiResponse.ok(assetService.getAssets(memberId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssetLinkResponse>> link(
            @LoginMember Long memberId) {
        return ResponseEntity.created(ASSET_LOCATION)
                .body(ApiResponse.ok(assetService.link(memberId)));
    }

    @PostMapping("/refresh")
    public ApiResponse<AssetLinkResponse> refresh(
            @LoginMember Long memberId) {
        return ApiResponse.ok(assetService.refresh(memberId));
    }
}
