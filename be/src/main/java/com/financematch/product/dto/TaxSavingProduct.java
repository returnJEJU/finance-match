package com.financematch.product.dto;

import com.financematch.product.type.IsaType;
import com.financematch.product.type.TaxAccountType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TaxSavingProduct {

    private Long productId;

    private String productName;
    private String description;
    private String url;
    private String companyName;

    private TaxAccountType accountType;

    // ISA가 아니면 null
    private IsaType isaType;
}