package com.financematch.recommendation.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.financematch.product.dto.TaxSavingProduct;
import com.financematch.product.mapper.TaxSavingMapper;
import com.financematch.product.type.IsaType;
import com.financematch.product.type.TaxAccountType;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.service.InvestmentRiskLevelCalculator;
import com.financematch.recommendation.type.RecommendationReasonCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxSavingRecommendationPolicyTest {

    @Mock private TaxSavingMapper taxSavingMapper;

    @Test
    void excludesOwnedAccountsAndAppliesPersonalIsaPreference() {
        when(taxSavingMapper.findAll())
                .thenReturn(
                        List.of(
                                product(1L, TaxAccountType.ISA, IsaType.BROKERAGE, "KB증권"),
                                product(
                                        2L,
                                        TaxAccountType.ISA,
                                        IsaType.DISCRETIONARY,
                                        "KB국민은행"),
                                product(3L, TaxAccountType.PENSION_SAVINGS, null, "KB증권"),
                                product(4L, TaxAccountType.IRP, null, "KB증권"),
                                product(5L, TaxAccountType.IRP, null, "KB국민은행")));
        TaxSavingRecommendationPolicy policy =
                new TaxSavingRecommendationPolicy(
                        taxSavingMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = context();
        context.setInviterHasPensionSaving(true);
        context.setInviteeHasIsa(true);

        var result = policy.recommend(context);

        assertEquals(List.of(4L, 5L, 2L, 1L), productIds(result.get(10L)));
        assertEquals(List.of(3L, 5L, 4L), productIds(result.get(20L)));
        assertTrue(result.get(10L).get(0).selected());
        assertEquals(
                RecommendationReasonCode.TAX_SAVING_IRP_INVESTMENT_TYPE,
                result.get(10L).get(0).reasonCode());
        assertFalse(result.get(10L).get(1).selected());
    }

    @Test
    void prioritizesIsaWhenRetirementIsSecondGoal() {
        when(taxSavingMapper.findAll())
                .thenReturn(
                        List.of(
                                product(1L, TaxAccountType.ISA, IsaType.BROKERAGE, "KB증권"),
                                product(2L, TaxAccountType.PENSION_SAVINGS, null, "KB증권"),
                                product(3L, TaxAccountType.IRP, null, "KB국민은행")));
        TaxSavingRecommendationPolicy policy =
                new TaxSavingRecommendationPolicy(
                        taxSavingMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = context();
        context.setFirstGoalType("HOUSING");
        context.setSecondGoalType("RETIREMENT");

        var result = policy.recommend(context);

        assertEquals(List.of(1L, 2L, 3L), productIds(result.get(10L)));
    }

    @Test
    void recommendsOnlyIsaWithoutRetirementGoalAndReturnsEmptyWhenIsaIsOwned() {
        when(taxSavingMapper.findAll())
                .thenReturn(
                        List.of(
                                product(1L, TaxAccountType.ISA, IsaType.BROKERAGE, "KB증권"),
                                product(2L, TaxAccountType.PENSION_SAVINGS, null, "KB증권"),
                                product(3L, TaxAccountType.IRP, null, "KB국민은행")));
        TaxSavingRecommendationPolicy policy =
                new TaxSavingRecommendationPolicy(
                        taxSavingMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = context();
        context.setFirstGoalType("HOUSING");
        context.setSecondGoalType("MARRIAGE");
        context.setInviterHasIsa(false);
        context.setInviteeHasIsa(true);
        context.setInviteeHasPensionSaving(true);
        context.setInviteeHasIrp(true);

        var result = policy.recommend(context);

        assertEquals(List.of(1L), productIds(result.get(10L)));
        assertTrue(result.get(20L).isEmpty());
    }

    @Test
    void filtersAccountTypesByEachMembersEligibility() {
        when(taxSavingMapper.findAll())
                .thenReturn(
                        List.of(
                                product(1L, TaxAccountType.ISA, IsaType.BROKERAGE, "KB증권"),
                                product(
                                        2L,
                                        TaxAccountType.PENSION_SAVINGS,
                                        null,
                                        "KB증권"),
                                product(3L, TaxAccountType.IRP, null, "KB국민은행")));
        TaxSavingRecommendationPolicy policy =
                new TaxSavingRecommendationPolicy(
                        taxSavingMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = context();
        context.setInviterTaxEligibilityStatus("INELIGIBLE");
        context.setInviterIsaEligibilityStatus("ELIGIBLE");
        context.setInviteeTaxEligibilityStatus("ELIGIBLE");
        context.setInviteeIsaEligibilityStatus("INELIGIBLE");

        var result = policy.recommend(context);

        assertEquals(List.of(1L), productIds(result.get(10L)));
        assertEquals(List.of(2L, 3L), productIds(result.get(20L)));
    }

    @Test
    void returnsEmptyWhenMemberIsNotEligibleForAnyTaxSavingAccount() {
        when(taxSavingMapper.findAll())
                .thenReturn(
                        List.of(
                                product(1L, TaxAccountType.ISA, IsaType.BROKERAGE, "KB증권"),
                                product(
                                        2L,
                                        TaxAccountType.PENSION_SAVINGS,
                                        null,
                                        "KB증권"),
                                product(3L, TaxAccountType.IRP, null, "KB국민은행")));
        TaxSavingRecommendationPolicy policy =
                new TaxSavingRecommendationPolicy(
                        taxSavingMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = context();
        context.setInviterTaxEligibilityStatus("INELIGIBLE");
        context.setInviterIsaEligibilityStatus("INELIGIBLE");

        var result = policy.recommend(context);

        assertTrue(result.get(10L).isEmpty());
    }

    @Test
    void rejectsUnknownEligibilityStatusBeforeQueryingProducts() {
        TaxSavingRecommendationPolicy policy =
                new TaxSavingRecommendationPolicy(
                        taxSavingMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = context();
        context.setInviterTaxEligibilityStatus("UNKNOWN");

        assertThrows(IllegalArgumentException.class, () -> policy.recommend(context));
        verifyNoInteractions(taxSavingMapper);
    }

    private RecommendationContext context() {
        RecommendationContext context = new RecommendationContext();
        context.setInviterId(10L);
        context.setInviteeId(20L);
        context.setFirstGoalType("RETIREMENT");
        context.setSecondGoalType("HOUSING");
        context.setInviterFinancialKnowledge("LOW");
        context.setInviteeFinancialKnowledge("HIGH");
        context.setInviterInvestmentType("AGGRESSIVE");
        context.setInviteeInvestmentType("STABLE");
        context.setInviterTaxEligibilityStatus("ELIGIBLE");
        context.setInviterIsaEligibilityStatus("ELIGIBLE");
        context.setInviteeTaxEligibilityStatus("ELIGIBLE");
        context.setInviteeIsaEligibilityStatus("ELIGIBLE");
        return context;
    }

    private TaxSavingProduct product(
            Long productId,
            TaxAccountType accountType,
            IsaType isaType,
            String companyName) {
        TaxSavingProduct product = new TaxSavingProduct();
        product.setProductId(productId);
        product.setAccountType(accountType);
        product.setIsaType(isaType);
        product.setCompanyName(companyName);
        return product;
    }

    private List<Long> productIds(List<RecommendedProduct> products) {
        return products.stream().map(RecommendedProduct::productId).toList();
    }
}
