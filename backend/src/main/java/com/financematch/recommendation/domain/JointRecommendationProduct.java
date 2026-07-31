package com.financematch.recommendation.domain;

import com.financematch.product.type.LoanPurpose;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JointRecommendationProduct {

    private Long slotId;
    private RecommendationSlotType slotType;
    private Long productId;
    private int rank;
    private boolean selected;
    private String productName;
    private String description;
    private String productUrl;
    private BigDecimal applicableBaseRate;
    private Integer riskLevel;
    private Integer aum;
    private BigDecimal loanMaxRate;
    private LoanPurpose loanPurpose;
}
