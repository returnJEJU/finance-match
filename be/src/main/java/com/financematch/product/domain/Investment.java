package com.financematch.product.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Investment {

    private Long id;

    // 1 ~ 6
    private Integer riskLevel;

    // 현재 seed 기준 억원 단위 정수값
    private Integer aum;

    private Boolean isTdf;
}