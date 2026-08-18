package com.financematch.report.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.report.mapper.ReportWriteMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalFeasibilityScoreServiceTest {

    @Mock private GoalFeasibilityReasonFormatter formatter;
    @Mock private ReportWriteMapper reportWriteMapper;

    @InjectMocks private GoalFeasibilityScoreService service;

    @Test
    void 포매터가_만든_문구와_예상자산을_그대로_저장한다() {
        BigDecimal expectedAsset = new BigDecimal("120000000");
        BigDecimal targetAmount = new BigDecimal("100000000");
        when(formatter.format(expectedAsset, targetAmount, 60)).thenReturn("목표 초과 달성");

        service.generateAndSave(1L, expectedAsset, targetAmount, 60);

        verify(reportWriteMapper).upsertGoalFeasibilityReason(1L, "목표 초과 달성", expectedAsset);
    }
}
