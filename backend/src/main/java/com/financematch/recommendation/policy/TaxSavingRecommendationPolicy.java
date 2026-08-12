package com.financematch.recommendation.policy;

import com.financematch.product.dto.TaxSavingProduct;
import com.financematch.product.mapper.TaxSavingMapper;
import com.financematch.product.type.IsaType;
import com.financematch.product.type.TaxAccountType;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.service.InvestmentRiskLevelCalculator;
import com.financematch.recommendation.type.PersonalRecommendationType;
import com.financematch.recommendation.type.RecommendationReasonCode;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaxSavingRecommendationPolicy implements PersonalRecommendationPolicy {

    private static final String ELIGIBLE = "ELIGIBLE";
    private static final String INELIGIBLE = "INELIGIBLE";

    private final TaxSavingMapper taxSavingMapper;
    private final InvestmentRiskLevelCalculator riskLevelCalculator;

    @Override
    public PersonalRecommendationType recommendationType() {
        return PersonalRecommendationType.TAX_SAVING;
    }

    @Override
    public Map<Long, List<RecommendedProduct>> recommend(RecommendationContext context) {
        validateEligibilityStatus(
                context.getInviterTaxEligibilityStatus(),
                context.getInviterIsaEligibilityStatus());
        validateEligibilityStatus(
                context.getInviteeTaxEligibilityStatus(),
                context.getInviteeIsaEligibilityStatus());

        List<TaxSavingProduct> products = taxSavingMapper.findAll();

        return Map.of(
                context.getInviterId(),
                recommendFor(
                        products,
                        context.getFirstGoalType(),
                        context.getSecondGoalType(),
                        context.isInviterHasPensionSaving(),
                        context.isInviterHasIrp(),
                        context.isInviterHasIsa(),
                        context.getInviterTaxEligibilityStatus(),
                        context.getInviterIsaEligibilityStatus(),
                        context.getInviterFinancialKnowledge(),
                        context.getInviterInvestmentType()),
                context.getInviteeId(),
                recommendFor(
                        products,
                        context.getFirstGoalType(),
                        context.getSecondGoalType(),
                        context.isInviteeHasPensionSaving(),
                        context.isInviteeHasIrp(),
                        context.isInviteeHasIsa(),
                        context.getInviteeTaxEligibilityStatus(),
                        context.getInviteeIsaEligibilityStatus(),
                        context.getInviteeFinancialKnowledge(),
                        context.getInviteeInvestmentType()));
    }

    private List<RecommendedProduct> recommendFor(
            List<TaxSavingProduct> products,
            String firstGoalType,
            String secondGoalType,
            boolean hasPensionSaving,
            boolean hasIrp,
            boolean hasIsa,
            String taxEligibilityStatus,
            String isaEligibilityStatus,
            String financialKnowledge,
            String investmentType) {
        List<TaxSavingProduct> rankedProducts = new ArrayList<>();
        for (TaxAccountType accountType : accountOrder(firstGoalType, secondGoalType)) {
            if (!isEligibleAccountType(
                    accountType, taxEligibilityStatus, isaEligibilityStatus)) {
                continue;
            }
            if (hasAccount(accountType, hasPensionSaving, hasIrp, hasIsa)) {
                continue;
            }
            products.stream()
                    .filter(product -> product.getAccountType() == accountType)
                    .sorted(
                            Comparator.comparingInt(
                                            (TaxSavingProduct product) ->
                                                    preference(
                                                            product,
                                                            financialKnowledge,
                                                            investmentType))
                                    .thenComparing(TaxSavingProduct::getProductId))
                    .forEach(rankedProducts::add);
        }

        return IntStream.range(0, rankedProducts.size())
                .mapToObj(
                        index ->
                                new RecommendedProduct(
                                        rankedProducts.get(index).getProductId(),
                                        index + 1,
                                        index == 0,
                                        index == 0
                                                ? reasonCode(rankedProducts.get(index).getAccountType())
                                                : null))
                .toList();
    }

    private RecommendationReasonCode reasonCode(TaxAccountType accountType) {
        return switch (accountType) {
            case ISA -> RecommendationReasonCode.TAX_SAVING_ISA_KNOWLEDGE;
            case PENSION_SAVINGS -> RecommendationReasonCode.TAX_SAVING_PENSION_RETIREMENT;
            case IRP -> RecommendationReasonCode.TAX_SAVING_IRP_INVESTMENT_TYPE;
        };
    }

    private boolean isEligibleAccountType(
            TaxAccountType accountType,
            String taxEligibilityStatus,
            String isaEligibilityStatus) {
        return switch (accountType) {
            case PENSION_SAVINGS, IRP -> ELIGIBLE.equals(taxEligibilityStatus);
            case ISA -> ELIGIBLE.equals(isaEligibilityStatus);
        };
    }

    private void validateEligibilityStatus(
            String taxEligibilityStatus, String isaEligibilityStatus) {
        if (!isKnownEligibilityStatus(taxEligibilityStatus)
                || !isKnownEligibilityStatus(isaEligibilityStatus)) {
            throw new IllegalArgumentException("절세 평가 대상 여부를 확인할 수 없습니다.");
        }
    }

    private boolean isKnownEligibilityStatus(String status) {
        return ELIGIBLE.equals(status) || INELIGIBLE.equals(status);
    }

    private List<TaxAccountType> accountOrder(String firstGoalType, String secondGoalType) {
        if ("RETIREMENT".equals(firstGoalType)) {
            return List.of(
                    TaxAccountType.PENSION_SAVINGS, TaxAccountType.IRP, TaxAccountType.ISA);
        }
        if ("RETIREMENT".equals(secondGoalType)) {
            return List.of(
                    TaxAccountType.ISA, TaxAccountType.PENSION_SAVINGS, TaxAccountType.IRP);
        }
        return List.of(TaxAccountType.ISA);
    }

    private boolean hasAccount(
            TaxAccountType accountType,
            boolean hasPensionSaving,
            boolean hasIrp,
            boolean hasIsa) {
        return switch (accountType) {
            case PENSION_SAVINGS -> hasPensionSaving;
            case IRP -> hasIrp;
            case ISA -> hasIsa;
        };
    }

    private int preference(
            TaxSavingProduct product, String financialKnowledge, String investmentType) {
        if (product.getAccountType() == TaxAccountType.ISA) {
            boolean lowKnowledge =
                    "VERY_LOW".equals(financialKnowledge) || "LOW".equals(financialKnowledge);
            IsaType preferredType = lowKnowledge ? IsaType.DISCRETIONARY : IsaType.BROKERAGE;
            return product.getIsaType() == preferredType ? 0 : 1;
        }
        if (product.getAccountType() == TaxAccountType.IRP) {
            Set<Integer> allowedRiskLevels = riskLevelCalculator.allowedRiskLevels(investmentType);
            boolean investmentOriented =
                    allowedRiskLevels.stream().min(Integer::compareTo).orElseThrow() <= 4;
            boolean securitiesCompany =
                    product.getCompanyName() != null && product.getCompanyName().contains("증권");
            return investmentOriented == securitiesCompany ? 0 : 1;
        }
        return 0;
    }
}
