package com.financematch.recommendation.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.financematch.product.dto.LoanProduct;
import com.financematch.product.mapper.LoanMapper;
import com.financematch.product.type.ApplicationChannel;
import com.financematch.product.type.IncomeBasis;
import com.financematch.product.type.LoanPurpose;
import com.financematch.product.type.LoanTargetGroup;
import com.financematch.recommendation.domain.RecommendationContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanRecommendationPolicyCoverageTest {

    @Mock private LoanMapper loanMapper;

    @Test
    void returnsEmptyForNullOrBlankLoanPurpose() {
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);
        RecommendationContext nullPurpose = context();
        nullPurpose.setLoanPurpose(null);
        RecommendationContext blankPurpose = context();
        blankPurpose.setLoanPurpose(" ");

        assertTrue(policy.recommend(nullPurpose).isEmpty());
        assertTrue(policy.recommend(blankPurpose).isEmpty());
        verifyNoInteractions(loanMapper);
    }

    @Test
    void rejectsMissingBirthDate() {
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);
        RecommendationContext context = context();
        context.setInviterBirthDate(null);

        assertThrows(IllegalArgumentException.class, () -> policy.recommend(context));
    }

    @Test
    void acceptsWhenOnlyInviteeMeetsAgeAndSupportsOpenAgeRange() {
        LoanProduct inviteeOnly = product(1L, LoanTargetGroup.GENERAL, 30, 35, null, null,
                ApplicationChannel.MOBILE, "4.00");
        LoanProduct unrestricted = product(2L, LoanTargetGroup.GENERAL, null, null, null, null,
                ApplicationChannel.MOBILE, "4.10");
        when(loanMapper.findByPurpose(LoanPurpose.JEONSE))
                .thenReturn(List.of(inviteeOnly, unrestricted));
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);
        RecommendationContext context = context();
        context.setInviterBirthDate(LocalDate.now(ZoneOffset.UTC).minusYears(40));
        context.setInviteeBirthDate(LocalDate.now(ZoneOffset.UTC).minusYears(32));

        assertEquals(List.of(1L, 2L), productIds(policy.recommend(context)));
    }

    @Test
    void validatesAllIncomeEligibilityPaths() {
        LoanProduct couple = product(1L, LoanTargetGroup.GENERAL, null, null, IncomeBasis.COUPLE,
                100_000_000L, ApplicationChannel.MOBILE, "4.00");
        LoanProduct individualRejected = product(2L, LoanTargetGroup.GENERAL, null, null,
                IncomeBasis.INDIVIDUAL, 30_000_000L, ApplicationChannel.MOBILE, "4.00");
        LoanProduct unknownBasis = product(3L, LoanTargetGroup.GENERAL, null, null, null,
                100_000_000L, ApplicationChannel.MOBILE, "4.00");
        when(loanMapper.findByPurpose(LoanPurpose.JEONSE))
                .thenReturn(List.of(couple, individualRejected, unknownBasis));
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);

        assertEquals(List.of(1L), productIds(policy.recommend(context())));

        RecommendationContext missingInviterIncome = context();
        missingInviterIncome.setInviterAnnualIncome(null);
        assertThrows(IllegalArgumentException.class, () -> policy.recommend(missingInviterIncome));

        RecommendationContext missingInviteeIncome = context();
        missingInviteeIncome.setInviteeAnnualIncome(null);
        assertThrows(IllegalArgumentException.class, () -> policy.recommend(missingInviteeIncome));
    }

    @Test
    void ranksProductsWithMissingOptionalSortValuesLast() {
        LoanProduct complete = product(1L, LoanTargetGroup.GENERAL, null, null, null, null,
                ApplicationChannel.MOBILE, "4.00");
        LoanProduct missing = product(2L, null, null, null, null, null, null, null);
        when(loanMapper.findByPurpose(LoanPurpose.JEONSE)).thenReturn(List.of(missing, complete));
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);

        assertEquals(List.of(1L, 2L), productIds(policy.recommend(context())));
    }

    private RecommendationContext context() {
        RecommendationContext context = new RecommendationContext();
        context.setLoanPurpose("JEONSE");
        context.setInviterBirthDate(LocalDate.of(1990, 1, 1));
        context.setInviteeBirthDate(LocalDate.of(1992, 1, 1));
        context.setInviterAnnualIncome(BigDecimal.valueOf(40_000_000L));
        context.setInviteeAnnualIncome(BigDecimal.valueOf(40_000_000L));
        return context;
    }

    private LoanProduct product(Long id, LoanTargetGroup targetGroup, Integer minAge,
            Integer maxAge, IncomeBasis incomeBasis, Long maxIncome,
            ApplicationChannel channel, String maxRate) {
        LoanProduct product = new LoanProduct();
        product.setProductId(id);
        product.setLoanPurpose(LoanPurpose.JEONSE);
        product.setTargetGroup(targetGroup);
        product.setMinAge(minAge);
        product.setMaxAge(maxAge);
        product.setIncomeBasis(incomeBasis);
        product.setMaxIncome(maxIncome);
        product.setApplicationChannel(channel);
        product.setMaxRate(maxRate == null ? null : new BigDecimal(maxRate));
        return product;
    }

    private List<Long> productIds(List<RecommendedProduct> products) {
        return products.stream().map(RecommendedProduct::productId).toList();
    }
}
