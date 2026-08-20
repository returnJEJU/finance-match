package com.financematch.recommendation.policy.joint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.financematch.product.domain.DepositRate;
import com.financematch.product.dto.DepositProduct;
import com.financematch.product.mapper.DepositMapper;
import com.financematch.product.type.DepositType;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.RecommendedProduct;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepositRecommendationPolicyCoverageTest {

    @Mock private DepositMapper depositMapper;

    @Test
    void rejectsMissingBalanceAndInvalidTargetPeriodsBeforeQuery() {
        DepositRecommendationPolicy policy = new DepositRecommendationPolicy(depositMapper);
        RecommendationContext missingBalance = context(12);
        missingBalance.setAvailableBalance(null);
        RecommendationContext missingPeriod = context(null);
        RecommendationContext zeroPeriod = context(0);

        assertTrue(policy.recommend(missingBalance).isEmpty());
        assertTrue(policy.recommend(missingPeriod).isEmpty());
        assertTrue(policy.recommend(zeroPeriod).isEmpty());
        verifyNoInteractions(depositMapper);
    }

    @Test
    void ignoresProductsWithoutUsableTermsOrRates() {
        DepositProduct missingMinTerm = product(1L, null, 12, List.of(rate(1L, 1, 12, "3.0")));
        DepositProduct nullRates = product(2L, 1, 12, null);
        DepositProduct emptyRates = product(3L, 1, 12, List.of());
        DepositProduct invalidRates =
                product(
                        4L,
                        1,
                        12,
                        List.of(
                                rate(1L, null, 12, "3.0"),
                                rate(2L, 1, 12, null),
                                rate(3L, 13, null, "3.0"),
                                rate(4L, 1, 11, "3.0")));
        when(depositMapper.findByType(DepositType.DEPOSIT))
                .thenReturn(List.of(missingMinTerm, nullRates, emptyRates, invalidRates));

        assertTrue(new DepositRecommendationPolicy(depositMapper).recommend(context(12)).isEmpty());
    }

    @Test
    void supportsUnlimitedTermsAndRatesAndUsesStableTieBreakers() {
        DepositProduct higherId = product(2L, 1, null, List.of(rate(2L, 1, null, "3.0")));
        DepositProduct lowerId =
                product(
                        1L,
                        1,
                        null,
                        List.of(rate(1L, 1, null, "2.0"), rate(2L, 6, null, "3.0")));
        when(depositMapper.findByType(DepositType.DEPOSIT))
                .thenReturn(List.of(higherId, lowerId));

        List<RecommendedProduct> result =
                new DepositRecommendationPolicy(depositMapper).recommend(context(12));

        assertEquals(List.of(1L, 2L), result.stream().map(RecommendedProduct::productId).toList());
    }

    private RecommendationContext context(Integer period) {
        RecommendationContext context = new RecommendationContext();
        context.setAvailableBalance(BigDecimal.valueOf(9_100_000L));
        context.setTargetPeriodMonths(period);
        return context;
    }

    private DepositProduct product(Long id, Integer minTerm, Integer maxTerm, List<DepositRate> rates) {
        DepositProduct product = new DepositProduct();
        product.setProductId(id);
        product.setMinTerm(minTerm);
        product.setMaxTerm(maxTerm);
        product.setRates(rates);
        return product;
    }

    private DepositRate rate(Long id, Integer minTerm, Integer maxTerm, String baseRate) {
        DepositRate rate = new DepositRate();
        rate.setId(id);
        rate.setMinTerm(minTerm);
        rate.setMaxTerm(maxTerm);
        rate.setBaseRate(baseRate == null ? null : new BigDecimal(baseRate));
        return rate;
    }
}
