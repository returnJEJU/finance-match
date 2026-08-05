package com.financematch.asset.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssetLinkRow {

    private BigDecimal financialAsset;
    private BigDecimal totalDebt;
    private LocalDateTime linkedAt;
}
