package com.financematch.asset.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AssetLinkResponse(
        BigDecimal totalAsset,
        BigDecimal totalDebt,
        int assetCount,
        List<AssetSummaryResponse> summary,
        AssetAccountResponse accounts,
        LocalDateTime linkedAt) {

    public AssetLinkResponse {
        if (summary == null) {
            throw new IllegalArgumentException("자산 요약 목록이 필요합니다.");
        }
        summary = List.copyOf(summary);

        if (totalAsset == null
                || totalAsset.signum() < 0
                || totalDebt == null
                || totalDebt.signum() < 0
                || assetCount < 0
                || accounts == null
                || linkedAt == null) {
            throw new IllegalArgumentException("자산 연동 응답이 올바르지 않습니다.");
        }
    }
}
