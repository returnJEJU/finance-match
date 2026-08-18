package com.financematch.report.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.report.dto.reason.FinancialValueReasonInput;
import com.financematch.report.mapper.ReportWriteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialValueScoreServiceTest {

    @Mock private FinancialValueReasonService reasonService;
    @Mock private ReportWriteMapper reportWriteMapper;

    @InjectMocks private FinancialValueScoreService service;

    @Test
    void reasonService가_만든_문구를_그대로_저장한다() {
        FinancialValueReasonInput input = new FinancialValueReasonInput(3, 3, 3, 3, 2, 2, 2, 2);
        when(reasonService.generate(input)).thenReturn("두 분의 투자 가치관 일치도가 높아요.");

        service.generateAndSave(1L, input);

        verify(reportWriteMapper).upsertFinancialValueReason(1L, "두 분의 투자 가치관 일치도가 높아요.");
    }
}
