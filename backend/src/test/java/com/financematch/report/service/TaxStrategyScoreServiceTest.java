package com.financematch.report.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.report.dto.reason.TaxAccountInput;
import com.financematch.report.dto.reason.TaxSavingProfile;
import com.financematch.report.dto.reason.TaxStrategyReasonInput;
import com.financematch.report.mapper.ReportWriteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxStrategyScoreServiceTest {

    @Mock private TaxStrategyReasonService reasonService;
    @Mock private ReportWriteMapper reportWriteMapper;

    @InjectMocks private TaxStrategyScoreService service;

    @Test
    void reasonService가_만든_문구를_그대로_저장한다() {
        TaxSavingProfile profile =
                new TaxSavingProfile(
                        TaxAccountInput.unopened(), TaxAccountInput.unopened(), TaxAccountInput.unopened());
        TaxStrategyReasonInput input = new TaxStrategyReasonInput("김철수", "이영희", profile, profile);
        when(reasonService.generate(input)).thenReturn("절세 계좌를 더 활용하면 좋아요.");

        service.generateAndSave(1L, input);

        verify(reportWriteMapper).upsertTaxStrategyReason(1L, "절세 계좌를 더 활용하면 좋아요.");
    }
}
