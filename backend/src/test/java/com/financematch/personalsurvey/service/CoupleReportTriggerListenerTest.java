package com.financematch.personalsurvey.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.match.service.MatchService;
import com.financematch.personalsurvey.domain.ReadyCoupleInvestmentTypes;
import com.financematch.personalsurvey.event.PersonalSurveyCompletedEvent;
import com.financematch.personalsurvey.mapper.PersonalSurveyMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoupleReportTriggerListenerTest {

    @Mock
    private PersonalSurveyMapper personalSurveyMapper;

    @Mock
    private MatchService matchService;

    @InjectMocks
    private CoupleReportTriggerListener listener;

    @Test
    void 파트너까지_설문을_끝냈으면_궁합도_계산을_트리거한다() {
        when(personalSurveyMapper.findReadyCoupleInvestmentTypes(1L))
                .thenReturn(new ReadyCoupleInvestmentTypes());

        listener.onPersonalSurveyCompleted(new PersonalSurveyCompletedEvent(1L));

        verify(matchService).getOrCalculateCompatibilityResult(1L);
    }

    @Test
    void 파트너가_아직_설문을_안_끝냈으면_아무것도_하지_않는다() {
        when(personalSurveyMapper.findReadyCoupleInvestmentTypes(1L)).thenReturn(null);

        listener.onPersonalSurveyCompleted(new PersonalSurveyCompletedEvent(1L));

        verify(matchService, never()).getOrCalculateCompatibilityResult(1L);
    }

    @Test
    void 궁합도_계산_중_예외가_나도_전파하지_않는다() {
        when(personalSurveyMapper.findReadyCoupleInvestmentTypes(1L))
                .thenReturn(new ReadyCoupleInvestmentTypes());
        when(matchService.getOrCalculateCompatibilityResult(1L))
                .thenThrow(new IllegalStateException("계산 실패"));

        listener.onPersonalSurveyCompleted(new PersonalSurveyCompletedEvent(1L));
        // 예외 없이 반환되면 성공 — @Async 로 도는 fire-and-forget 이라 호출자에게 전파하면 안 된다.
    }
}
