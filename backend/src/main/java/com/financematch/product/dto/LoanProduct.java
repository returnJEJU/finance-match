package com.financematch.product.dto;

import com.financematch.product.type.ApplicationChannel;
import com.financematch.product.type.IncomeBasis;
import com.financematch.product.type.LoanPurpose;
import com.financematch.product.type.LoanTargetGroup;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LoanProduct {

    private Long productId;

    private String productName;
    private String description;
    private String url;
    private String companyName;

    private LoanPurpose loanPurpose;
    private LoanTargetGroup targetGroup;

    private Integer minAge;
    private Integer maxAge;

    private IncomeBasis incomeBasis;
    private Long maxIncome;

    private ApplicationChannel applicationChannel;

    private BigDecimal maxRate;
}