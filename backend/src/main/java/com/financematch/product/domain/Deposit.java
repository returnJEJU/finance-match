package com.financematch.product.domain;

import com.financematch.product.type.DepositType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Deposit {

    private Long id;

    private DepositType type;

    private Integer minTerm;
    private Integer maxTerm;

    private Integer savingMin;
    private Integer savingMax;

    private BigDecimal minRate;
    private BigDecimal maxRate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}