package com.financematch.asset.dto;

public record AssetAccountResponse(
        boolean hasPensionSavings,
        boolean hasIrp,
        boolean hasIsa) {
}
