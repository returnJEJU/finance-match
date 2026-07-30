package com.financematch.recommendation.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Recommendation {

    // recommendation.id: 커플별 최신 추천 결과의 부모 row ID
    private Long id;

    // recommendation.couple_id: 추천 결과를 소유한 커플 ID
    private Long coupleId;

    public Recommendation(Long coupleId) {
        this.coupleId = coupleId;
    }
}
