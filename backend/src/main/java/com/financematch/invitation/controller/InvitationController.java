package com.financematch.invitation.controller;

import com.financematch.common.ApiResponse;
import com.financematch.invitation.dto.CommonSurveyResponse;
import com.financematch.invitation.dto.CreateInvitationRequest;
import com.financematch.invitation.dto.CreateInvitationResponse;
import com.financematch.invitation.dto.GetInvitationResponse;
import com.financematch.invitation.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/v1/members/me/invitation")
@RequiredArgsConstructor
public class InvitationController {

    private static final Long TEMP_MEMBER_ID = 3L;

    private static final URI INVITATION_LOCATION = URI.create("/api/v1/members/me/invitation");

    private final InvitationService invitationService;

    @PostMapping("")
    public ResponseEntity<ApiResponse<CreateInvitationResponse>> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request) {
        // @RequestBody로 DTO 변환 (JSON -> Java 객체)
        // @Valid로 DTO 검증 (Java 객체에 선언된 @NotNull 등 실행; @Valid가 없으면 자동 검증되지 않음)
        // 검증 성공 시 Controller 메서드 실행
        // 검증 실패 시 MethodArgumentNotValidException

        // TODO: JWT 구현 후 임시 ID를 제거하고 인증된 회원 ID를 주입받는다.
        // 예:
        // createInvitation(
        //     @LoginMember Long memberId,
        //     @Valid @RequestBody CreateInvitationRequest request)
        Long memberId = TEMP_MEMBER_ID;

        CreateInvitationResponse response = invitationService.createInvitation(memberId, request);

        return ResponseEntity
                .created(INVITATION_LOCATION)
                .body(ApiResponse.ok(response));
    }

    @GetMapping("")
    public ApiResponse<GetInvitationResponse> getInvitation() {
        // TODO: JWT 구현 후 임시 ID를 제거하고 인증된 회원 ID를 주입받는다.
        // 예: getInvitation(@LoginMember Long memberId)
        Long memberId = TEMP_MEMBER_ID;

        GetInvitationResponse response = invitationService.getInvitation(memberId);

        return ApiResponse.ok(response);
    }

    @GetMapping("/common-survey")
    public ApiResponse<CommonSurveyResponse> getCommonSurvey() {
        // TODO: JWT 구현 후 임시 ID 대신 인증된 회원 ID를 주입받는다.
        // 예: getCommonSurvey(@LoginMember Long memberId)
        Long memberId = TEMP_MEMBER_ID;

        CommonSurveyResponse response = invitationService.getCommonSurvey(memberId);

        return ApiResponse.ok(response);
    }

}
