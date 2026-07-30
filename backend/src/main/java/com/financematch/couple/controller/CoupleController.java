package com.financematch.couple.controller;

import com.financematch.common.ApiResponse;
import com.financematch.couple.dto.CreateCoupleRequest;
import com.financematch.couple.dto.CreateCoupleResponse;
import com.financematch.couple.service.CoupleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/v1/members/me/couple")
@RequiredArgsConstructor
public class CoupleController {

    private static final Long TEMP_MEMBER_ID = 4L;
    private static final URI COUPLE_LOCATION = URI.create("/api/v1/members/me/couple");

    private final CoupleService coupleService;

    @PostMapping("")
    public ResponseEntity<ApiResponse<CreateCoupleResponse>> createCouple(
            @Valid @RequestBody CreateCoupleRequest request) {

        // TODO: JWT 구현 후 임시 ID를 제거하고 인증된 회원 ID를 주입받는다.
        // 예:
        // createCouple(
        //     @LoginMember Long memberId,
        //     @Valid @RequestBody CreateCoupleRequest request)
        Long memberId = TEMP_MEMBER_ID;

        CreateCoupleResponse response = coupleService.createCouple(memberId, request);

        return ResponseEntity.created(COUPLE_LOCATION)
                .body(ApiResponse.ok(response));
    }
}
