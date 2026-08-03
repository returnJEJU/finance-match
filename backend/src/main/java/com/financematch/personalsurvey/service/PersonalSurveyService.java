package com.financematch.personalsurvey.service;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.personalsurvey.calculator.PersonalInvestmentCalculationInput;
import com.financematch.personalsurvey.calculator.PersonalInvestmentTypeCalculator;
import com.financematch.personalsurvey.domain.PersonalInvestmentType;
import com.financematch.personalsurvey.domain.PersonalSurvey;
import com.financematch.personalsurvey.domain.PersonalSurveyCalculationContext;
import com.financematch.personalsurvey.domain.PersonalSurveyResult;
import com.financematch.personalsurvey.dto.PersonalSurveyRequest;
import com.financematch.personalsurvey.dto.PersonalSurveyResponse;
import com.financematch.personalsurvey.event.PersonalSurveyCompletedEvent;
import com.financematch.personalsurvey.mapper.PersonalSurveyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PersonalSurveyService {

    private final PersonalSurveyMapper personalSurveyMapper;
    private final PersonalInvestmentTypeCalculator calculator;
    private final CoupleInvestmentTypeService coupleInvestmentTypeService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void save(Long memberId, PersonalSurveyRequest request) {

        Long lockedMemberId = personalSurveyMapper.lockActiveMemberById(memberId);
        if (lockedMemberId == null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        if (personalSurveyMapper.existsPersonalSurveyByMemberId(memberId)) {
            // TODO: 개인 설문 수정 기능 추가한다면 예외 대신 기존 설문 및 투자성향을 갱신
            throw new ApiException(ErrorCode.PERSONAL_SURVEY_ALREADY_EXISTS);
        }

        PersonalSurveyCalculationContext context = personalSurveyMapper.findCalculationContext(memberId);
        if (context == null) {
            throw new ApiException(ErrorCode.COMMON_SURVEY_NOT_FOUND);
        }

        PersonalInvestmentCalculationInput input =
                PersonalInvestmentCalculationInput.builder()
                        .birthDate(context.getBirthDate())
                        .annualIncome(request.getAnnualIncome().longValueExact())
                        .financialAssetRatio(request.getFinancialAssetRatio())
                        .investmentExperiences(request.getInvestmentExperiences())
                        .financialKnowledge(request.getFinancialKnowledge())
                        .capitalPreservationAttitude(request.getCapitalPreservationAttitude())
                        .firstGoalType(context.getFirstGoalType())
                        .targetPeriodMonths(context.getTargetPeriodMonths())
                        .build();

        PersonalInvestmentType investmentType = calculator.calculate(input, LocalDate.now());

        PersonalSurvey personalSurvey = PersonalSurvey.of(memberId, request);

        int surveyInserted = personalSurveyMapper.insertPersonalSurvey(personalSurvey);
        if (surveyInserted != 1 || personalSurvey.getId() == null) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }

        int experiencesInserted = personalSurveyMapper.insertInvestmentExperiences(
                        personalSurvey.getId(),
                        request.getInvestmentExperiences());
        if (experiencesInserted != request.getInvestmentExperiences().size()) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }

        int memberUpdated = personalSurveyMapper
                .updateMemberInvestmentType(memberId, investmentType);
        if (memberUpdated != 1) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }

        coupleInvestmentTypeService.calculateAndSaveIfReady(memberId);

        // 커플 궁합도(리포트) 계산 트리거. 파트너까지 끝났는지는 트랜잭션 커밋 후
        // CoupleReportTriggerListener 가 직접 다시 조회해 판단한다.
        eventPublisher.publishEvent(new PersonalSurveyCompletedEvent(memberId));
    }

    public PersonalSurveyResponse getPersonalSurveyResult(Long memberId) {
        PersonalSurveyResult result =
                personalSurveyMapper.findPersonalSurveyResultByMemberId(memberId);

        if (result == null) {
            throw new ApiException(ErrorCode.MEMBER_NOT_FOUND);
        }

        if (result.getInvestmentType() == null) {
            throw new ApiException(ErrorCode.INVESTMENT_PROFILE_NOT_FOUND);
        }

        return PersonalSurveyResponse.of(result);
    }
}
