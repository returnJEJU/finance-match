package com.financematch.personalsurvey.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.personalsurvey.calculator.CoupleInvestmentTypeCalculator;
import com.financematch.personalsurvey.domain.CoupleInvestmentType;
import com.financematch.personalsurvey.domain.PersonalInvestmentType;
import com.financematch.personalsurvey.domain.ReadyCoupleInvestmentTypes;
import com.financematch.personalsurvey.mapper.PersonalSurveyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoupleInvestmentTypeServiceTest {

    @Mock
    private PersonalSurveyMapper personalSurveyMapper;

    @Mock
    private CoupleInvestmentTypeCalculator calculator;

    private CoupleInvestmentTypeService coupleInvestmentTypeService;

    @BeforeEach
    void setUp() {
        coupleInvestmentTypeService =
                new CoupleInvestmentTypeService(personalSurveyMapper, calculator);
    }

    /** 커플이 연동되지 않았거나 상대방의 개인 설문이 완료되지 않으면 계산과 저장을 하지 않는지 검증한다. */
    @Test
    void doesNothingWhenCoupleIsNotReady() {
        when(personalSurveyMapper.findReadyCoupleInvestmentTypes(1L)).thenReturn(null);

        coupleInvestmentTypeService.calculateAndSaveIfReady(1L);

        verify(calculator, never())
                .calculate(any(PersonalInvestmentType.class), any(PersonalInvestmentType.class));
        verify(personalSurveyMapper, never())
                .updateCoupleInvestmentType(any(), any(CoupleInvestmentType.class));
    }

    /** 두 사람의 개인 투자성향이 준비되면 커플 투자성향을 계산해 해당 커플에 저장하는지 검증한다. */
    @Test
    void calculatesAndSavesCoupleInvestmentType() {
        ReadyCoupleInvestmentTypes couple = readyCoupleInvestmentTypes();
        when(personalSurveyMapper.findReadyCoupleInvestmentTypes(1L)).thenReturn(couple);
        when(calculator.calculate(
                        PersonalInvestmentType.STABLE,
                        PersonalInvestmentType.NEUTRAL))
                .thenReturn(CoupleInvestmentType.DIFF_2);
        when(personalSurveyMapper.updateCoupleInvestmentType(10L, CoupleInvestmentType.DIFF_2))
                .thenReturn(1);

        coupleInvestmentTypeService.calculateAndSaveIfReady(1L);

        verify(calculator)
                .calculate(PersonalInvestmentType.STABLE, PersonalInvestmentType.NEUTRAL);
        verify(personalSurveyMapper)
                .updateCoupleInvestmentType(10L, CoupleInvestmentType.DIFF_2);
    }

    /** 계산한 커플 투자성향의 UPDATE 결과가 1이 아니면 INTERNAL_ERROR 예외가 발생하는지 검증한다. */
    @Test
    void throwsWhenCoupleInvestmentTypeUpdateFails() {
        ReadyCoupleInvestmentTypes couple = readyCoupleInvestmentTypes();
        when(personalSurveyMapper.findReadyCoupleInvestmentTypes(1L)).thenReturn(couple);
        when(calculator.calculate(
                        PersonalInvestmentType.STABLE,
                        PersonalInvestmentType.NEUTRAL))
                .thenReturn(CoupleInvestmentType.DIFF_2);
        when(personalSurveyMapper.updateCoupleInvestmentType(10L, CoupleInvestmentType.DIFF_2))
                .thenReturn(0);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleInvestmentTypeService.calculateAndSaveIfReady(1L));

        assertEquals(ErrorCode.INTERNAL_ERROR, exception.getErrorCode());
    }

    private ReadyCoupleInvestmentTypes readyCoupleInvestmentTypes() {
        ReadyCoupleInvestmentTypes couple = mock(ReadyCoupleInvestmentTypes.class);
        when(couple.getCoupleId()).thenReturn(10L);
        when(couple.getInviterInvestmentType()).thenReturn(PersonalInvestmentType.STABLE);
        when(couple.getInviteeInvestmentType()).thenReturn(PersonalInvestmentType.NEUTRAL);
        return couple;
    }
}
