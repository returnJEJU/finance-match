package com.financematch.personalsurvey.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.invitation.domain.GoalType;
import com.financematch.personalsurvey.calculator.PersonalInvestmentCalculationInput;
import com.financematch.personalsurvey.calculator.PersonalInvestmentTypeCalculator;
import com.financematch.personalsurvey.domain.FinancialAssetRatio;
import com.financematch.personalsurvey.domain.PersonalInvestmentType;
import com.financematch.personalsurvey.domain.PersonalSurvey;
import com.financematch.personalsurvey.domain.PersonalSurveyCalculationContext;
import com.financematch.personalsurvey.domain.PersonalSurveyResult;
import com.financematch.personalsurvey.dto.PersonalSurveyRequest;
import com.financematch.personalsurvey.dto.PersonalSurveyResponse;
import com.financematch.personalsurvey.event.PersonalSurveyCompletedEvent;
import com.financematch.personalsurvey.mapper.PersonalSurveyMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PersonalSurveyServiceTest {

    @Mock
    private PersonalSurveyMapper personalSurveyMapper;

    @Mock
    private PersonalInvestmentTypeCalculator calculator;

    @Mock
    private CoupleInvestmentTypeService coupleInvestmentTypeService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PersonalSurveyService personalSurveyService;

    @BeforeEach
    void setUp() {
        personalSurveyService =
                new PersonalSurveyService(
                        personalSurveyMapper,
                        calculator,
                        coupleInvestmentTypeService,
                        eventPublisher);
    }

    @Test
    void savesPersonalSurveyWhenMemberSubmitsForFirstTime() throws Exception {
        PersonalSurveyRequest request = request();
        PersonalSurveyCalculationContext context = calculationContext();
        when(personalSurveyMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(personalSurveyMapper.findCalculationContext(1L)).thenReturn(context);
        when(calculator.calculate(any(PersonalInvestmentCalculationInput.class), any(LocalDate.class)))
                .thenReturn(PersonalInvestmentType.STABLE_SEEKING);
        when(personalSurveyMapper.findPersonalSurveyIdByMemberId(1L)).thenReturn(null);
        when(personalSurveyMapper.insertPersonalSurvey(any(PersonalSurvey.class)))
                .thenAnswer(
                        invocation -> {
                            PersonalSurvey survey = invocation.getArgument(0);
                            survey.setId(11L);
                            return 1;
                        });
        when(personalSurveyMapper.insertInvestmentExperiences(11L, request.getInvestmentExperiences()))
                .thenReturn(request.getInvestmentExperiences().size());
        when(personalSurveyMapper.updateMemberInvestmentType(1L, PersonalInvestmentType.STABLE_SEEKING))
                .thenReturn(1);

        personalSurveyService.save(1L, request);

        verify(personalSurveyMapper).insertPersonalSurvey(any(PersonalSurvey.class));
        verify(personalSurveyMapper, never()).updatePersonalSurvey(any(PersonalSurvey.class));
        verify(personalSurveyMapper, never()).deleteInvestmentExperiences(11L);
        verify(personalSurveyMapper)
                .insertInvestmentExperiences(11L, request.getInvestmentExperiences());
        verify(coupleInvestmentTypeService).calculateAndSaveIfReady(1L);
        verify(eventPublisher).publishEvent(any(PersonalSurveyCompletedEvent.class));
    }

    @Test
    void replacesPersonalSurveyAndExperiencesWhenMemberResubmits() throws Exception {
        PersonalSurveyRequest request = request();
        PersonalSurveyCalculationContext context = calculationContext();
        when(personalSurveyMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(personalSurveyMapper.findCalculationContext(1L)).thenReturn(context);
        when(calculator.calculate(any(PersonalInvestmentCalculationInput.class), any(LocalDate.class)))
                .thenReturn(PersonalInvestmentType.AGGRESSIVE);
        when(personalSurveyMapper.findPersonalSurveyIdByMemberId(1L)).thenReturn(11L);
        when(personalSurveyMapper.updatePersonalSurvey(any(PersonalSurvey.class))).thenReturn(1);
        when(personalSurveyMapper.insertInvestmentExperiences(11L, request.getInvestmentExperiences()))
                .thenReturn(request.getInvestmentExperiences().size());
        when(personalSurveyMapper.updateMemberInvestmentType(1L, PersonalInvestmentType.AGGRESSIVE))
                .thenReturn(1);

        personalSurveyService.save(1L, request);

        ArgumentCaptor<PersonalSurvey> surveyCaptor = ArgumentCaptor.forClass(PersonalSurvey.class);
        verify(personalSurveyMapper).updatePersonalSurvey(surveyCaptor.capture());
        PersonalSurvey updatedSurvey = surveyCaptor.getValue();

        assertEquals(11L, updatedSurvey.getId());
        assertEquals(1L, updatedSurvey.getMemberId());
        assertEquals(request.getAnnualIncome(), updatedSurvey.getAnnualIncome());
        assertEquals(FinancialAssetRatio.UNDER_50, updatedSurvey.getFinancialAssetRatio());

        verify(personalSurveyMapper, never()).insertPersonalSurvey(any(PersonalSurvey.class));
        verify(personalSurveyMapper).deleteInvestmentExperiences(11L);
        verify(personalSurveyMapper)
                .insertInvestmentExperiences(11L, request.getInvestmentExperiences());
        verify(personalSurveyMapper).updateMemberInvestmentType(1L, PersonalInvestmentType.AGGRESSIVE);
        verify(coupleInvestmentTypeService).calculateAndSaveIfReady(1L);
        verify(eventPublisher).publishEvent(any(PersonalSurveyCompletedEvent.class));
    }

    /** 개인 설문 결과가 존재하면 회원 정보와 투자성향 설명을 응답으로 반환하는지 검증한다. */
    @Test
    void returnsPersonalSurveyResultWhenProfileExists() {
        PersonalSurveyResult result = mock(PersonalSurveyResult.class);
        when(result.getName()).thenReturn("김하나");
        when(result.getInvestmentType()).thenReturn(PersonalInvestmentType.STABLE_SEEKING);
        when(personalSurveyMapper.findPersonalSurveyResultByMemberId(1L)).thenReturn(result);

        PersonalSurveyResponse response = personalSurveyService.getPersonalSurveyResult(1L);

        assertEquals("김하나", response.getName());
        assertEquals(PersonalInvestmentType.STABLE_SEEKING.getKoreanName(), response.getInvestmentType());
        assertEquals(PersonalInvestmentType.STABLE_SEEKING.getHeadline(), response.getHeadline());
        assertEquals(PersonalInvestmentType.STABLE_SEEKING.getDescription(), response.getDescription());
    }

    /** 개인 설문 결과를 조회할 회원이 없으면 MEMBER_NOT_FOUND 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenGettingResultForMissingMember() {
        when(personalSurveyMapper.findPersonalSurveyResultByMemberId(1L)).thenReturn(null);

        ApiException exception = assertThrows(
                        ApiException.class,
                        () -> personalSurveyService.getPersonalSurveyResult(1L));

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    /** 회원은 있지만 투자성향 계산 결과가 없으면 INVESTMENT_PROFILE_NOT_FOUND 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenInvestmentProfileDoesNotExist() {
        PersonalSurveyResult result = mock(PersonalSurveyResult.class);
        when(personalSurveyMapper.findPersonalSurveyResultByMemberId(1L)).thenReturn(result);
        when(result.getInvestmentType()).thenReturn(null);

        ApiException exception = assertThrows(
                        ApiException.class,
                        () -> personalSurveyService.getPersonalSurveyResult(1L));

        assertEquals(ErrorCode.INVESTMENT_PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    /** 개인 설문을 저장할 회원이 없으면 이후 계산과 저장을 진행하지 않는지 검증한다. */
    @Test
    void throwsWhenSavingSurveyForMissingMember() throws Exception {
        PersonalSurveyRequest request = request();
        when(personalSurveyMapper.lockActiveMemberById(1L)).thenReturn(null);

        ApiException exception =
                assertThrows(ApiException.class, () -> personalSurveyService.save(1L, request));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(personalSurveyMapper, never()).findCalculationContext(1L);
        verify(calculator, never())
                .calculate(any(PersonalInvestmentCalculationInput.class), any(LocalDate.class));
        verify(personalSurveyMapper, never()).insertPersonalSurvey(any(PersonalSurvey.class));
    }

    /** 공통 설문이 없으면 개인 투자성향 계산과 개인 설문 저장을 진행하지 않는지 검증한다. */
    @Test
    void throwsWhenCommonSurveyDoesNotExist() throws Exception {
        PersonalSurveyRequest request = request();
        when(personalSurveyMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(personalSurveyMapper.findCalculationContext(1L)).thenReturn(null);

        ApiException exception =
                assertThrows(ApiException.class, () -> personalSurveyService.save(1L, request));

        assertEquals(ErrorCode.COMMON_SURVEY_NOT_FOUND, exception.getErrorCode());
        verify(calculator, never())
                .calculate(any(PersonalInvestmentCalculationInput.class), any(LocalDate.class));
        verify(personalSurveyMapper, never()).insertPersonalSurvey(any(PersonalSurvey.class));
    }

    /** 최초 개인 설문 INSERT 결과가 1이 아니면 INTERNAL_ERROR 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenPersonalSurveyInsertFails() throws Exception {
        PersonalSurveyRequest request = request();
        prepareSaveBeforePersistence(PersonalInvestmentType.STABLE_SEEKING);
        when(personalSurveyMapper.findPersonalSurveyIdByMemberId(1L)).thenReturn(null);
        when(personalSurveyMapper.insertPersonalSurvey(any(PersonalSurvey.class))).thenReturn(0);

        ApiException exception =
                assertThrows(ApiException.class, () -> personalSurveyService.save(1L, request));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verify(personalSurveyMapper, never())
                .insertInvestmentExperiences(any(), any());
        verify(coupleInvestmentTypeService, never()).calculateAndSaveIfReady(1L);
        verify(eventPublisher, never()).publishEvent(any(PersonalSurveyCompletedEvent.class));
    }

    /** 개인 설문 INSERT는 성공했지만 생성된 ID가 없으면 INTERNAL_ERROR 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenInsertedPersonalSurveyHasNoId() throws Exception {
        PersonalSurveyRequest request = request();
        prepareSaveBeforePersistence(PersonalInvestmentType.STABLE_SEEKING);
        when(personalSurveyMapper.findPersonalSurveyIdByMemberId(1L)).thenReturn(null);
        when(personalSurveyMapper.insertPersonalSurvey(any(PersonalSurvey.class))).thenReturn(1);

        ApiException exception =
                assertThrows(ApiException.class, () -> personalSurveyService.save(1L, request));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verify(personalSurveyMapper, never())
                .insertInvestmentExperiences(any(), any());
        verify(coupleInvestmentTypeService, never()).calculateAndSaveIfReady(1L);
        verify(eventPublisher, never()).publishEvent(any(PersonalSurveyCompletedEvent.class));
    }

    /** 기존 개인 설문 UPDATE 결과가 1이 아니면 후속 저장을 중단하는지 검증한다. */
    @Test
    void throwsWhenPersonalSurveyUpdateFails() throws Exception {
        PersonalSurveyRequest request = request();
        prepareSaveBeforePersistence(PersonalInvestmentType.AGGRESSIVE);
        when(personalSurveyMapper.findPersonalSurveyIdByMemberId(1L)).thenReturn(11L);
        when(personalSurveyMapper.updatePersonalSurvey(any(PersonalSurvey.class))).thenReturn(0);

        ApiException exception =
                assertThrows(ApiException.class, () -> personalSurveyService.save(1L, request));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verify(personalSurveyMapper, never()).deleteInvestmentExperiences(11L);
        verify(personalSurveyMapper, never())
                .insertInvestmentExperiences(any(), any());
        verify(coupleInvestmentTypeService, never()).calculateAndSaveIfReady(1L);
        verify(eventPublisher, never()).publishEvent(any(PersonalSurveyCompletedEvent.class));
    }

    /** 요청한 투자 경험 수와 실제 INSERT 수가 다르면 후속 처리를 중단하는지 검증한다. */
    @Test
    void throwsWhenInvestmentExperienceInsertCountDoesNotMatch() throws Exception {
        PersonalSurveyRequest request = request();
        prepareSaveBeforePersistence(PersonalInvestmentType.STABLE_SEEKING);
        prepareSuccessfulSurveyInsert();
        when(personalSurveyMapper.insertInvestmentExperiences(11L, request.getInvestmentExperiences()))
                .thenReturn(request.getInvestmentExperiences().size() - 1);

        ApiException exception =
                assertThrows(ApiException.class, () -> personalSurveyService.save(1L, request));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verify(personalSurveyMapper, never())
                .updateMemberInvestmentType(1L, PersonalInvestmentType.STABLE_SEEKING);
        verify(coupleInvestmentTypeService, never()).calculateAndSaveIfReady(1L);
        verify(eventPublisher, never()).publishEvent(any(PersonalSurveyCompletedEvent.class));
    }

    /** 회원 투자성향 UPDATE가 실패하면 커플 성향 계산과 완료 이벤트 발행을 하지 않는지 검증한다. */
    @Test
    void throwsWhenMemberInvestmentTypeUpdateFails() throws Exception {
        PersonalSurveyRequest request = request();
        prepareSaveBeforePersistence(PersonalInvestmentType.STABLE_SEEKING);
        prepareSuccessfulSurveyInsert();
        when(personalSurveyMapper.insertInvestmentExperiences(11L, request.getInvestmentExperiences()))
                .thenReturn(request.getInvestmentExperiences().size());
        when(personalSurveyMapper.updateMemberInvestmentType(1L, PersonalInvestmentType.STABLE_SEEKING))
                .thenReturn(0);

        ApiException exception =
                assertThrows(ApiException.class, () -> personalSurveyService.save(1L, request));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
        verify(coupleInvestmentTypeService, never()).calculateAndSaveIfReady(1L);
        verify(eventPublisher, never()).publishEvent(any(PersonalSurveyCompletedEvent.class));
    }

    private void prepareSaveBeforePersistence(PersonalInvestmentType investmentType) {
        PersonalSurveyCalculationContext context = calculationContext();

        when(personalSurveyMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(personalSurveyMapper.findCalculationContext(1L)).thenReturn(context);
        when(calculator.calculate(any(PersonalInvestmentCalculationInput.class), any(LocalDate.class)))
                .thenReturn(investmentType);
    }

    private void prepareSuccessfulSurveyInsert() {
        when(personalSurveyMapper.findPersonalSurveyIdByMemberId(1L)).thenReturn(null);
        when(personalSurveyMapper.insertPersonalSurvey(any(PersonalSurvey.class)))
                .thenAnswer(
                        invocation -> {
                            PersonalSurvey survey = invocation.getArgument(0);
                            survey.setId(11L);
                            return 1;
                        });
    }

    private PersonalSurveyCalculationContext calculationContext() {
        PersonalSurveyCalculationContext context = mock(PersonalSurveyCalculationContext.class);
        when(context.getBirthDate()).thenReturn(LocalDate.of(1995, 3, 21));
        when(context.getFirstGoalType()).thenReturn(GoalType.INVESTMENT);
        when(context.getTargetPeriodMonths()).thenReturn(36);
        return context;
    }

    private PersonalSurveyRequest request() throws Exception {
        return new ObjectMapper()
                .readValue(
                        """
                        {
                          "annualIncome": 45000000,
                          "monthlyAvailableAmount": 1000000,
                          "financialAssetRatio": "UNDER_50",
                          "investmentExperiences": ["LOW_RISK", "MODERATE_LOW_RISK"],
                          "financialKnowledge": "MEDIUM",
                          "capitalPreservationAttitude": "UNDER_50"
                        }
                        """,
                        PersonalSurveyRequest.class);
    }
}
