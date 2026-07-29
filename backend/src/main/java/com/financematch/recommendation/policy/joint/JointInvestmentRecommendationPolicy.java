package com.financematch.recommendation.policy.joint;

import com.financematch.product.dto.InvestmentProduct;
import com.financematch.product.mapper.InvestmentMapper;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.service.InvestmentRiskLevelCalculator;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JointInvestmentRecommendationPolicy implements JointRecommendationPolicy {

    private final InvestmentMapper investmentMapper;
    private final InvestmentRiskLevelCalculator riskLevelCalculator;

    @Override
    public RecommendationSlotType slotType() {
        return RecommendationSlotType.INVESTMENT;
    }

    @Override
    public List<RecommendedProduct> recommend(RecommendationContext context) {
        if (context.isHasLoanWithinOneMonth()) {
            return List.of();
        }

        Set<Integer> allowedRiskLevels =
                riskLevelCalculator.conservativeAllowedRiskLevels(
                        context.getInviterInvestmentType(), context.getInviteeInvestmentType());
        boolean prioritizeTdf = "RETIREMENT".equals(context.getFirstGoalType());
        List<InvestmentProduct> rankedProducts =
                investmentMapper.findAll().stream()
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
