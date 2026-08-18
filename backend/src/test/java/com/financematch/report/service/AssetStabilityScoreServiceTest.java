package com.financematch.report.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.report.mapper.ReportWriteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetStabilityScoreServiceTest {

    @Mock private AssetStabilityReasonFormatter formatter;
    @Mock private ReportWriteMapper reportWriteMapper;

    @InjectMocks private AssetStabilityScoreService service;

    @Test
    void 포매터가_만든_문구를_그대로_저장한다() {
        when(formatter.format(1.3)).thenReturn("또래 커플 대비 안정적인 편이에요.");

        service.generateAndSave(1L, 1.3);

        verify(reportWriteMapper).upsertAssetStabilityReason(1L, "또래 커플 대비 안정적인 편이에요.");
    }
}
