package com.financematch.product.domain;

import com.financematch.product.type.IsaType;
import com.financematch.product.type.TaxAccountType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TaxSaving {

    private Long id;

    private TaxAccountType accountType;

    // ISA가 아니면 null
    private IsaType isaType;
}