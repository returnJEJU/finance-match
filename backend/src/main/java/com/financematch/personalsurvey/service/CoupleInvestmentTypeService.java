package com.financematch.personalsurvey.service;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.personalsurvey.calculator.CoupleInvestmentTypeCalculator;
import com.financematch.personalsurvey.domain.CoupleInvestmentType;
import com.financematch.personalsurvey.domain.ReadyCoupleInvestmentTypes;
import com.financematch.personalsurvey.mapper.PersonalSurveyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoupleInvestmentTypeService {

    private final PersonalSurveyMapper personalSurveyMapper;
    private final CoupleInvestmentTypeCalculator calculator;

    public void calculateAndSaveIfReady(Long memberId) {

        ReadyCoupleInvestmentTypes couple = personalSurveyMapper
                .findReadyCoupleInvestmentTypes(memberId);

        // 커플 미연동 또는 파트너가 개인 설문 미완료
        if (couple == null) {
            return;
        }

        CoupleInvestmentType result = calculator.calculate(
                couple.getInviterInvestmentType(),
                couple.getInviteeInvestmentType());

        int updated = personalSurveyMapper
                .updateCoupleInvestmentType(couple.getCoupleId(), result);

        if (updated != 1) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }

    }

}
