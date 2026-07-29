package com.financematch.recommendation.policy;

import com.financematch.product.dto.InvestmentProduct;
import com.financematch.product.mapper.InvestmentMapper;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.service.InvestmentRiskLevelCalculator;
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
        List<InvestmentProduct> products = investmentMapper.findAll();

        return Map.of(
                context.getInviterId(),
                recommendFor(
                        context.getInviterInvestmentType(),
                        context.getFirstGoalType(),
                        products),
                context.getInviteeId(),
                recommendFor(
                        context.getInviteeInvestmentType(),
                        context.getFirstGoalType(),
                        products));
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
                                        index == 0))
                .toList();
    }
}
