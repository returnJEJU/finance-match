package com.financematch.recommendation.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalInvestmentProduct {

    private Long targetMemberId;
    private String targetMemberName;
    private Long productId;
    private int rank;
    private String productName;
    private String description;
    private String productUrl;
    private int riskLevel;
}
