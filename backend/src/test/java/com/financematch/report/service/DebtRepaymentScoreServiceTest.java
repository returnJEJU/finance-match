package com.financematch.report.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.report.dto.reason.DebtRepaymentReasonInput;
import com.financematch.report.mapper.ReportWriteMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DebtRepaymentScoreServiceTest {

    @Mock private DebtRepaymentReasonService reasonService;
    @Mock private ReportWriteMapper reportWriteMapper;

    @InjectMocks private DebtRepaymentScoreService service;

    @Test
    void reasonService가_만든_문구를_그대로_저장한다() {
        DebtRepaymentReasonInput input =
                new DebtRepaymentReasonInput(
                        "김철수", "이영희", true, false, new BigDecimal("62.50"), new BigDecimal("100"));
        when(reasonService.generate(input)).thenReturn("철수님이 부채 상환에 더 신경 쓰면 좋아요.");

        service.generateAndSave(1L, input);

        verify(reportWriteMapper).upsertDebtRepaymentReason(1L, "철수님이 부채 상환에 더 신경 쓰면 좋아요.");
    }
}
