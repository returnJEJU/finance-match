package com.financematch.product.dto;

import com.financematch.product.domain.DepositRate;
import com.financematch.product.type.DepositType;
import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DepositProduct {

    private Long productId;

    private String productName;
    private String description;
    private String url;
    private String companyName;

    private DepositType depositType;

    private Integer minTerm;
    private Integer maxTerm;

    private Integer savingMin;
    private Integer savingMax;

    private BigDecimal minRate;
    private BigDecimal maxRate;

    // 기간별 기본금리
    private List<DepositRate> rates;
}