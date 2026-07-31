package com.financematch.personalsurvey.controller;

import com.financematch.common.ApiResponse;
import com.financematch.personalsurvey.dto.PersonalSurveyRequest;
import com.financematch.personalsurvey.service.PersonalSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/members/me/personal-survey")
@RequiredArgsConstructor
public class PersonalSurveyController {

    private static final Long TEMP_MEMBER_ID = 4L;

    private final PersonalSurveyService personalSurveyService;

    @PutMapping("")
    public ApiResponse<Void> savePersonalSurvey(
            @Valid @RequestBody PersonalSurveyRequest request) {
        personalSurveyService.save(TEMP_MEMBER_ID, request);
        return ApiResponse.ok(null);
    }

}
