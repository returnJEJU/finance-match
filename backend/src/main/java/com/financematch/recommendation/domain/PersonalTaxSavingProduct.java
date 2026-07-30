package com.financematch.recommendation.domain;

import com.financematch.product.type.TaxAccountType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalTaxSavingProduct {

    private Long targetMemberId;
    private String targetMemberName;
    private Long productId;
    private int rank;
    private String productName;
    private String description;
    private String productUrl;
    private TaxAccountType accountType;
}
