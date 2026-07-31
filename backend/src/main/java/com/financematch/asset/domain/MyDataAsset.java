package com.financematch.asset.domain;

import java.math.BigDecimal;

public record MyDataAsset(
        String institutionName,
        String productName,
        AssetCategory category,
        BigDecimal balance) {

    public MyDataAsset {
        if (institutionName == null
                || institutionName.isBlank()
                || productName == null
                || productName.isBlank()
                || category == null
                || balance == null
                || balance.signum() < 0) {
            throw new IllegalArgumentException("마이데이터 자산 정보가 올바르지 않습니다.");
        }
    }
}
