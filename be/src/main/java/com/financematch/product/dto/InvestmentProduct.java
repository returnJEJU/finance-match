package com.financematch.product.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InvestmentProduct {

    private Long productId;

    private String productName;
    private String description;
    private String url;
    private String companyName;

    private Integer riskLevel;
    private Integer aum;
    private Boolean isTdf;
}