package com.financematch.personalsurvey.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.invitation.domain.GoalType;
import com.financematch.personalsurvey.calculator.PersonalInvestmentCalculationInput;
import com.financematch.personalsurvey.calculator.PersonalInvestmentTypeCalculator;
import com.financematch.personalsurvey.domain.FinancialAssetRatio;
import com.financematch.personalsurvey.domain.PersonalInvestmentType;
import com.financematch.personalsurvey.domain.PersonalSurvey;
import com.financematch.personalsurvey.domain.PersonalSurveyCalculationContext;
import com.financematch.personalsurvey.dto.PersonalSurveyRequest;
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
