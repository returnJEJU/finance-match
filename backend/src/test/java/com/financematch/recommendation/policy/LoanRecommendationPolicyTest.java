package com.financematch.recommendation.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanRecommendationPolicyTest {

    @Mock private LoanMapper loanMapper;

    @Test
    void filtersEligibilityAndRanksNewlywedProductFirst() {
        when(loanMapper.findByPurpose(LoanPurpose.JEONSE))
                .thenReturn(
                        List.of(
                                product(
                                        1L,
                                        LoanTargetGroup.GENERAL,
                                        19,
                                        null,
                                        null,
                                        null,
                                        ApplicationChannel.MOBILE,
                                        "3.00"),
                                product(
                                        2L,
                                        LoanTargetGroup.NEWLYWED,
                                        19,
                                        null,
                                        IncomeBasis.COUPLE,
                                        100_000_000L,
                                        ApplicationChannel.BRANCH,
                                        "4.00"),
                                product(
                                        3L,
                                        LoanTargetGroup.OTHER,
                                        19,
                                        20,
                                        null,
                                        null,
                                        ApplicationChannel.MOBILE,
                                        "2.00")));
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);
        RecommendationContext context = context();

        List<RecommendedProduct> result = policy.recommend(context);

        assertEquals(List.of(2L, 1L), productIds(result));
        assertTrue(result.get(0).selected());
    }

    @Test
    void returnsEmptyForHighRateDebtWithoutQueryingProducts() {
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);
        RecommendationContext context = context();
        context.setHasHighRateDebt(true);

        assertTrue(policy.recommend(context).isEmpty());
        verifyNoInteractions(loanMapper);
    }

    @Test
    void returnsEmptyForNoLoanPurposeWithoutQueryingProducts() {
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);
        RecommendationContext context = context();
        context.setLoanPurpose("NONE");

        assertTrue(policy.recommend(context).isEmpty());
        verifyNoInteractions(loanMapper);
    }

    @Test
    void rejectsUnknownLoanPurpose() {
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);
        RecommendationContext context = context();
        context.setLoanPurpose("UNKNOWN");

        assertThrows(IllegalArgumentException.class, () -> policy.recommend(context));
        verifyNoInteractions(loanMapper);
    }

    @Test
    void acceptsIndividualIncomeLimitWhenEitherMemberQualifies() {
        LoanProduct product =
                product(
                        1L,
                        LoanTargetGroup.GENERAL,
                        19,
                        null,
                        IncomeBasis.INDIVIDUAL,
                        50_000_000L,
                        ApplicationChannel.MOBILE,
                        "4.00");
        when(loanMapper.findByPurpose(LoanPurpose.JEONSE)).thenReturn(List.of(product));
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);
        RecommendationContext context = context();
        context.setInviterAnnualIncome(BigDecimal.valueOf(70_000_000L));
        context.setInviteeAnnualIncome(BigDecimal.valueOf(40_000_000L));

        List<RecommendedProduct> result = policy.recommend(context);

        assertEquals(List.of(1L), productIds(result));
    }

    @Test
    void excludesProductWhenCoupleIncomeExceedsLimit() {
        LoanProduct product =
                product(
                        1L,
                        LoanTargetGroup.GENERAL,
                        19,
                        null,
                        IncomeBasis.COUPLE,
                        70_000_000L,
                        ApplicationChannel.MOBILE,
                        "4.00");
        when(loanMapper.findByPurpose(LoanPurpose.JEONSE)).thenReturn(List.of(product));
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);

        assertTrue(policy.recommend(context()).isEmpty());
    }

    @Test
    void includesMemberWhoseAgeEqualsProductMaximumAge() {
        int inviterAge = java.time.Year.now(java.time.ZoneOffset.UTC).getValue() - 1990;
        LoanProduct product =
                product(
                        1L,
                        LoanTargetGroup.GENERAL,
                        inviterAge,
                        inviterAge,
                        null,
                        null,
                        ApplicationChannel.MOBILE,
                        "4.00");
        when(loanMapper.findByPurpose(LoanPurpose.JEONSE)).thenReturn(List.of(product));
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);
        RecommendationContext context = context();
        context.setInviterBirthDate(LocalDate.of(1990, 1, 1));
        context.setInviteeBirthDate(LocalDate.of(1980, 1, 1));

        assertEquals(List.of(1L), productIds(policy.recommend(context)));
    }

    @Test
    void ranksLowerRateBeforeChannelAndMobileBeforeOnlineOnEqualRate() {
        LoanProduct branchLowRate =
                product(
                        1L,
                        LoanTargetGroup.GENERAL,
                        19,
                        null,
                        null,
                        null,
                        ApplicationChannel.BRANCH,
                        "3.00");
        LoanProduct online =
                product(
                        2L,
                        LoanTargetGroup.GENERAL,
                        19,
                        null,
                        null,
                        null,
                        ApplicationChannel.ONLINE,
                        "4.00");
        LoanProduct mobile =
                product(
                        3L,
                        LoanTargetGroup.GENERAL,
                        19,
                        null,
                        null,
                        null,
                        ApplicationChannel.MOBILE,
                        "4.00");
        when(loanMapper.findByPurpose(LoanPurpose.JEONSE))
                .thenReturn(List.of(online, branchLowRate, mobile));
        LoanRecommendationPolicy policy = new LoanRecommendationPolicy(loanMapper);

        List<RecommendedProduct> result = policy.recommend(context());

        assertEquals(List.of(1L, 3L, 2L), productIds(result));
        verify(loanMapper).findByPurpose(LoanPurpose.JEONSE);
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

    private LoanProduct product(
            Long productId,
            LoanTargetGroup targetGroup,
            Integer minAge,
            Integer maxAge,
            IncomeBasis incomeBasis,
            Long maxIncome,
            ApplicationChannel channel,
            String maxRate) {
        LoanProduct product = new LoanProduct();
        product.setProductId(productId);
        product.setLoanPurpose(LoanPurpose.JEONSE);
        product.setTargetGroup(targetGroup);
        product.setMinAge(minAge);
        product.setMaxAge(maxAge);
        product.setIncomeBasis(incomeBasis);
        product.setMaxIncome(maxIncome);
        product.setApplicationChannel(channel);
        product.setMaxRate(new BigDecimal(maxRate));
        return product;
    }

    private List<Long> productIds(List<RecommendedProduct> products) {
        return products.stream().map(RecommendedProduct::productId).toList();
    }
}
