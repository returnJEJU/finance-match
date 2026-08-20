package com.financematch.recommendation.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.financematch.product.domain.DepositRate;
import com.financematch.product.dto.DepositProduct;
import com.financematch.product.dto.LoanProduct;
import com.financematch.product.mapper.DepositMapper;
import com.financematch.product.mapper.LoanMapper;
import com.financematch.product.type.ApplicationChannel;
import com.financematch.product.type.DepositType;
import com.financematch.product.type.LoanPurpose;
import com.financematch.product.type.LoanTargetGroup;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.dto.PackageProductResponse;
import com.financematch.recommendation.policy.joint.SavingsRecommendationPolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationFinalBranchCoverageTest {

    @Mock private LoanMapper loanMapper;
    @Mock private DepositMapper depositMapper;

    @Test
    void packageProductRejectsNullUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PackageProductResponse(
                                1L, "상품", null, null, null,
                                "기본금리", "연 3%", null, null, null, null));
    }

    @Test
    void loanRejectsWhenBothMembersAreYoungerThanMinimumAge() {
        LoanProduct product = loan(1L, 50, ApplicationChannel.MOBILE, "4.00");
        when(loanMapper.findByPurpose(LoanPurpose.JEONSE)).thenReturn(List.of(product));

        assertTrue(new LoanRecommendationPolicy(loanMapper).recommend(loanContext()).isEmpty());
    }

    @Test
    void loanRanksNullChannelLastAndCoversBranchChannel() {
        LoanProduct mobile = loan(1L, null, ApplicationChannel.MOBILE, "4.00");
        LoanProduct branch = loan(2L, null, ApplicationChannel.BRANCH, "4.00");
        LoanProduct missing = loan(3L, null, null, "4.00");
        when(loanMapper.findByPurpose(LoanPurpose.JEONSE))
                .thenReturn(List.of(missing, branch, mobile));

        List<Long> ids = new LoanRecommendationPolicy(loanMapper).recommend(loanContext()).stream()
                .map(RecommendedProduct::productId)
                .toList();

        assertEquals(List.of(1L, 2L, 3L), ids);
    }

    @Test
    void savingsUsesProductIdAsFinalTieBreaker() {
        DepositProduct second = savings(2L);
        DepositProduct first = savings(1L);
        when(depositMapper.findByType(DepositType.SAVINGS)).thenReturn(List.of(second, first));
        RecommendationContext context = new RecommendationContext();
        context.setTargetPeriodMonths(12);
        context.setInviterMonthlyAvailableAmount(BigDecimal.ONE);
        context.setInviteeMonthlyAvailableAmount(BigDecimal.ONE);

        List<Long> ids = new SavingsRecommendationPolicy(depositMapper).recommend(context).stream()
                .map(RecommendedProduct::productId)
                .toList();

        assertEquals(List.of(1L, 2L), ids);
    }

    private RecommendationContext loanContext() {
        RecommendationContext context = new RecommendationContext();
        context.setLoanPurpose("JEONSE");
        context.setInviterBirthDate(LocalDate.now(ZoneOffset.UTC).minusYears(30));
        context.setInviteeBirthDate(LocalDate.now(ZoneOffset.UTC).minusYears(31));
        context.setInviterAnnualIncome(BigDecimal.ONE);
        context.setInviteeAnnualIncome(BigDecimal.ONE);
        return context;
    }

    private LoanProduct loan(Long id, Integer minAge, ApplicationChannel channel, String rate) {
        LoanProduct product = new LoanProduct();
        product.setProductId(id);
        product.setTargetGroup(LoanTargetGroup.GENERAL);
        product.setMinAge(minAge);
        product.setApplicationChannel(channel);
        product.setMaxRate(new BigDecimal(rate));
        return product;
    }

    private DepositProduct savings(Long id) {
        DepositRate rate = new DepositRate();
        rate.setId(id);
        rate.setMinTerm(1);
        rate.setMaxTerm(12);
        rate.setBaseRate(new BigDecimal("3.00"));
        DepositProduct product = new DepositProduct();
        product.setProductId(id);
        product.setProductName("일반 적금 " + id);
        product.setMinTerm(1);
        product.setMaxTerm(12);
        product.setRates(List.of(rate));
        return product;
    }
}
