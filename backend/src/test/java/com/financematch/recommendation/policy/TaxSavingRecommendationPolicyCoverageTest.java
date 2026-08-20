package com.financematch.recommendation.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.financematch.product.dto.TaxSavingProduct;
import com.financematch.product.mapper.TaxSavingMapper;
import com.financematch.product.type.IsaType;
import com.financematch.product.type.TaxAccountType;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.service.InvestmentRiskLevelCalculator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxSavingRecommendationPolicyCoverageTest {

    @Mock private TaxSavingMapper taxSavingMapper;

    @Test
    void rejectsUnknownIsaStatusAfterValidTaxStatus() {
        TaxSavingRecommendationPolicy policy =
                new TaxSavingRecommendationPolicy(taxSavingMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = context();
        context.setInviterIsaEligibilityStatus("UNKNOWN");

        assertThrows(IllegalArgumentException.class, () -> policy.recommend(context));
        verifyNoInteractions(taxSavingMapper);
    }

    @Test
    void coversVeryLowIsaAndAllIrpAndPensionPreferencePaths() {
        when(taxSavingMapper.findAll())
                .thenReturn(
                        List.of(
                                product(1L, TaxAccountType.ISA, IsaType.DISCRETIONARY, null),
                                product(2L, TaxAccountType.ISA, IsaType.BROKERAGE, null),
                                product(3L, TaxAccountType.IRP, null, null),
                                product(4L, TaxAccountType.IRP, null, "KB국민은행"),
                                product(5L, TaxAccountType.PENSION_SAVINGS, null, null),
                                product(6L, TaxAccountType.PENSION_SAVINGS, null, "KB증권")));
        TaxSavingRecommendationPolicy policy =
                new TaxSavingRecommendationPolicy(taxSavingMapper, new InvestmentRiskLevelCalculator());
        RecommendationContext context = context();
        context.setInviterFinancialKnowledge("VERY_LOW");
        context.setInviterInvestmentType("STABLE");

        var result = policy.recommend(context);

        assertEquals(6, result.get(10L).size());
    }

    private RecommendationContext context() {
        RecommendationContext context = new RecommendationContext();
        context.setInviterId(10L);
        context.setInviteeId(20L);
        context.setFirstGoalType("RETIREMENT");
        context.setSecondGoalType("HOUSING");
        context.setInviterFinancialKnowledge("HIGH");
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
            Long id, TaxAccountType type, IsaType isaType, String company) {
        TaxSavingProduct product = new TaxSavingProduct();
        product.setProductId(id);
        product.setAccountType(type);
        product.setIsaType(isaType);
        product.setCompanyName(company);
        return product;
    }
}
