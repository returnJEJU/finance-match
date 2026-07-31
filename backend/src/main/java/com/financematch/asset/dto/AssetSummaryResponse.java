package com.financematch.asset.dto;

import com.financematch.asset.domain.AssetCategory;
import java.math.BigDecimal;

public record AssetSummaryResponse(AssetCategory category, BigDecimal amount) {

    public AssetSummaryResponse {
        if (category == null || amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("자산 요약 응답이 올바르지 않습니다.");
        }
    }
}
