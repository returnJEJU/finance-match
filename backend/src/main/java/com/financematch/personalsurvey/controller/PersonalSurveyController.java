package com.financematch.personalsurvey.controller;

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

    // TODO: 추후 @LoginMember Long memberId로 교체
    private static final Long TEMP_MEMBER_ID = 3L;

    private final PersonalSurveyService personalSurveyService;

    @PutMapping("")
    public ApiResponse<Void> savePersonalSurvey(
            @Valid @RequestBody PersonalSurveyRequest request) {
        personalSurveyService.save(TEMP_MEMBER_ID, request);
        return ApiResponse.ok(null);
    }

    @GetMapping("")
    public ApiResponse<PersonalSurveyResponse> getPersonalSurveyResult() {
        PersonalSurveyResponse response = personalSurveyService.getPersonalSurveyResult(TEMP_MEMBER_ID);
        return ApiResponse.ok(response);
    }

}
