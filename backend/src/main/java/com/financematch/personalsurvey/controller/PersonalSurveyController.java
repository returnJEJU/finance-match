package com.financematch.personalsurvey.controller;

import com.financematch.auth.annotation.LoginMember;
import com.financematch.common.ApiResponse;
import com.financematch.personalsurvey.dto.PersonalSurveyRequest;
import com.financematch.personalsurvey.dto.PersonalSurveyResponse;
import com.financematch.personalsurvey.service.PersonalSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/members/me/personal-survey")
@RequiredArgsConstructor
public class PersonalSurveyController {

    private final PersonalSurveyService personalSurveyService;

    @PutMapping("")
    public ApiResponse<Void> savePersonalSurvey(
            @LoginMember Long memberId,
            @Valid @RequestBody PersonalSurveyRequest request) {
        personalSurveyService.save(memberId, request);
        return ApiResponse.ok(null);
    }

    @GetMapping("")
    public ApiResponse<PersonalSurveyResponse> getPersonalSurveyResult(@LoginMember Long memberId) {
        PersonalSurveyResponse response = personalSurveyService.getPersonalSurveyResult(memberId);
        return ApiResponse.ok(response);
    }

}
