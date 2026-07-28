package com.financematch.product.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DepositRate {

    private Long id;

    private Long depositId;

    private Integer minTerm;
    private Integer maxTerm;

    private BigDecimal baseRate;

//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
}