package com.financematch.recommendation.policy;

import com.financematch.product.dto.InvestmentProduct;
import com.financematch.product.mapper.InvestmentMapper;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.service.InvestmentRiskLevelCalculator;
import com.financematch.recommendation.type.RecommendationReasonCode;
import com.financematch.recommendation.type.PersonalRecommendationType;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonalInvestmentRecommendationPolicy implements PersonalRecommendationPolicy {

    private final InvestmentMapper investmentMapper;
    private final InvestmentRiskLevelCalculator riskLevelCalculator;

    @Override
    public PersonalRecommendationType recommendationType() {
        return PersonalRecommendationType.INVESTMENT;
    }

    @Override
    public Map<Long, List<RecommendedProduct>> recommend(RecommendationContext context) {
        if (context.isHasLoanWithinOneMonth()) {
            return Map.of();
        }

        Long targetMemberId = moreAggressiveMemberId(context);
        if (targetMemberId == null) {
            return Map.of();
        }

        List<InvestmentProduct> products = investmentMapper.findAll();
        String targetInvestmentType =
                targetMemberId.equals(context.getInviterId())
                        ? context.getInviterInvestmentType()
                        : context.getInviteeInvestmentType();

        return Map.of(
                targetMemberId,
                recommendFor(
                        targetInvestmentType,
                        context.getFirstGoalType(),
                        products));
    }

    private Long moreAggressiveMemberId(RecommendationContext context) {
        int inviterRiskLevel = highestAllowedRiskLevel(context.getInviterInvestmentType());
        int inviteeRiskLevel = highestAllowedRiskLevel(context.getInviteeInvestmentType());

        if (inviterRiskLevel == inviteeRiskLevel) {
            return null;
        }
        return inviterRiskLevel < inviteeRiskLevel
                ? context.getInviterId()
                : context.getInviteeId();
    }

    private int highestAllowedRiskLevel(String investmentType) {
        return riskLevelCalculator.allowedRiskLevels(investmentType).stream()
                .min(Integer::compareTo)
                .orElseThrow();
    }

    private List<RecommendedProduct> recommendFor(
            String investmentType, String firstGoalType, List<InvestmentProduct> products) {
        Set<Integer> allowedRiskLevels = riskLevelCalculator.allowedRiskLevels(investmentType);
        boolean prioritizeTdf = "RETIREMENT".equals(firstGoalType);
        List<InvestmentProduct> rankedProducts =
                products.stream()
                        .filter(product -> product.getRiskLevel() != null)
                        .filter(product -> allowedRiskLevels.contains(product.getRiskLevel()))
                        .sorted(
                                Comparator.comparingInt(
                                                (InvestmentProduct product) ->
                                                        prioritizeTdf
                                                                        && Boolean.TRUE.equals(
                                                                                product.getIsTdf())
                                                                ? 0
                                                                : 1)
                                        .thenComparingInt(InvestmentProduct::getRiskLevel)
                                        .thenComparing(
                                                InvestmentProduct::getAum,
                                                Comparator.nullsLast(Comparator.reverseOrder()))
                                        .thenComparing(InvestmentProduct::getProductId))
                        .toList();

        return IntStream.range(0, rankedProducts.size())
                .mapToObj(
                        index ->
                                new RecommendedProduct(
                                        rankedProducts.get(index).getProductId(),
                                        index + 1,
                                        index == 0,
                                        index == 0
                                                ? (prioritizeTdf
                                                                && Boolean.TRUE.equals(
                                                                        rankedProducts.get(index).getIsTdf())
                                                        ? RecommendationReasonCode.INVESTMENT_RETIREMENT_TDF_PRIORITY
                                                        : RecommendationReasonCode.INVESTMENT_RISK_AND_AUM)
                                                : null))
                .toList();
    }
}
