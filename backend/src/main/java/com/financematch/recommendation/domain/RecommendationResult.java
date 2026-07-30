package com.financematch.recommendation.domain;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationResult {

    private Long recommendationId;
    private Long coupleId;
    private LocalDateTime updatedAt;
    private boolean hasHighInterestDebt;
}
