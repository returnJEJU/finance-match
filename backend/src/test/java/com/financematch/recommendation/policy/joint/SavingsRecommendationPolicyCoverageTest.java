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
class SavingsRecommendationPolicyCoverageTest {

    @Mock private DepositMapper depositMapper;

    @Test
    void rejectsInvalidPeriodAndUnavailableMonthlyAmountsBeforeQuery() {
        SavingsRecommendationPolicy policy = new SavingsRecommendationPolicy(depositMapper);
        assertTrue(policy.recommend(context(null, "1", "1")).isEmpty());
        assertTrue(policy.recommend(context(0, "1", "1")).isEmpty());
        assertTrue(policy.recommend(context(12, null, "1")).isEmpty());
        assertTrue(policy.recommend(context(12, "1", null)).isEmpty());
        assertTrue(policy.recommend(context(12, "-1", "1")).isEmpty());
        verifyNoInteractions(depositMapper);
    }

    @Test
    void ignoresProductsWithoutUsableTermsRatesOrMinimumAmount() {
        DepositProduct missingMinTerm = product(1L, "일반", null, 12, null,
                List.of(rate(1L, 1, 12, "3.0")));
        DepositProduct tooExpensive = product(2L, "일반", 1, 12, 3,
                List.of(rate(2L, 1, 12, "3.0")));
        DepositProduct noRates = product(3L, "일반", 1, 12, null, null);
        DepositProduct emptyRates = product(4L, "일반", 1, 12, null, List.of());
        DepositProduct invalidRates = product(5L, "일반", 1, 12, null,
                List.of(rate(1L, null, 12, "3.0"), rate(2L, 1, 12, null),
                        rate(3L, 13, null, "3.0"), rate(4L, 1, 11, "3.0")));
        when(depositMapper.findByType(DepositType.SAVINGS))
                .thenReturn(List.of(missingMinTerm, tooExpensive, noRates, emptyRates, invalidRates));

        assertTrue(new SavingsRecommendationPolicy(depositMapper)
                .recommend(context(12, "1", "1")).isEmpty());
    }

    @Test
    void supportsInviteeKbEligibilityUnlimitedTermsAndStableOrdering() {
        DepositProduct star = product(3L, "KB스타적금Ⅲ", 1, null, null,
                List.of(rate(1L, 1, null, "1.0")));
        DepositProduct general = product(2L, "일반", 1, null, null,
                List.of(rate(1L, 1, null, "2.0"), rate(2L, 6, null, "3.0")));
        when(depositMapper.findByType(DepositType.SAVINGS)).thenReturn(List.of(general, star));
        RecommendationContext context = context(12, "1", "1");
        context.setInviteeKbStarSavingsEligible(true);

        List<RecommendedProduct> result =
                new SavingsRecommendationPolicy(depositMapper).recommend(context);

        assertEquals(List.of(3L, 2L), result.stream().map(RecommendedProduct::productId).toList());
    }

    @Test
    void excludesGeneralEPlusAtExactSplitAmount() {
        DepositProduct ePlus = product(1L, "KB일반 e-plus정기적금", 1, 12, null,
                List.of(rate(1L, 1, 12, "3.0")));
        when(depositMapper.findByType(DepositType.SAVINGS)).thenReturn(List.of(ePlus));

        assertTrue(new SavingsRecommendationPolicy(depositMapper)
                .recommend(context(12, "500000", "500000")).isEmpty());
    }

    private RecommendationContext context(Integer period, String inviter, String invitee) {
        RecommendationContext context = new RecommendationContext();
        context.setTargetPeriodMonths(period);
        context.setInviterMonthlyAvailableAmount(inviter == null ? null : new BigDecimal(inviter));
        context.setInviteeMonthlyAvailableAmount(invitee == null ? null : new BigDecimal(invitee));
        return context;
    }

    private DepositProduct product(Long id, String name, Integer minTerm, Integer maxTerm,
            Integer savingMin, List<DepositRate> rates) {
        DepositProduct product = new DepositProduct();
        product.setProductId(id);
        product.setProductName(name);
        product.setMinTerm(minTerm);
        product.setMaxTerm(maxTerm);
        product.setSavingMin(savingMin);
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
