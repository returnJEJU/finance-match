package com.financematch.recommendation.policy.joint;

import java.util.ArrayList;
import java.util.Comparator;
import com.financematch.product.domain.DepositRate;
import java.util.Optional;
import com.financematch.product.dto.DepositProduct;
import com.financematch.product.mapper.DepositMapper;
import com.financematch.product.type.DepositType;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.policy.JointRecommendationPolicy;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DepositRecommendationPolicy
        implements JointRecommendationPolicy {

    private final DepositMapper depositMapper;

    public DepositRecommendationPolicy(
            DepositMapper depositMapper) {
        this.depositMapper = depositMapper;
    }

    @Override
    public RecommendationSlotType slotType() {
        return RecommendationSlotType.DEPOSIT;
    }

    @Override
    public List<RecommendedProduct> recommend(
            RecommendationContext context) {

        List<DepositProduct> candidates =
                depositMapper.findByType(DepositType.DEPOSIT);

        Integer targetPeriodMonths = context.getTargetPeriodMonths();

        if (targetPeriodMonths == null || targetPeriodMonths <= 0) {
            return List.of();
        }

        List<DepositCandidate> eligibleCandidates =
                candidates.stream()
                        .filter(product ->
                                supportsTerm(
                                        product,
                                        targetPeriodMonths))
                        .map(product ->
                                findApplicableRate(
                                        product,
                                        targetPeriodMonths)
                                        .map(rate ->
                                                new DepositCandidate(
                                                        product,
                                                        rate)))
                        .flatMap(Optional::stream)
                        .filter(candidate ->
                                candidate.rate().getBaseRate() != null)
                        .sorted(
                                Comparator.comparing(
                                                (DepositCandidate candidate) ->
                                                        candidate.rate().getBaseRate())
                                        .reversed()
                                        .thenComparing(
                                                candidate ->
                                                        candidate.product().getProductId()))
                        .toList();

        List<RecommendedProduct> recommendations = new ArrayList<>();

        int limit = Math.min(3, eligibleCandidates.size());

        for (int i = 0; i < limit; i++) {

            DepositCandidate candidate = eligibleCandidates.get(i);

            int rank = i + 1;
            boolean selected = rank == 1;

            recommendations.add(
                    new RecommendedProduct(
                            candidate.product().getProductId(),
                            rank,
                            selected
                    )
            );
        }

        return List.copyOf(recommendations);

    }
    private boolean supportsTerm(
            DepositProduct product,
            int targetPeriodMonths) {

        if (product.getMinTerm() == null
                || product.getMaxTerm() == null) {
            return false;
        }

        return targetPeriodMonths >= product.getMinTerm()
                && targetPeriodMonths <= product.getMaxTerm();
    }

    private Optional<DepositRate> findApplicableRate(
            DepositProduct product,
            int targetPeriodMonths) {

        if (product.getRates() == null || product.getRates().isEmpty()) {
            return Optional.empty();
        }

        return product.getRates().stream()
                .filter(rate ->
                        rate.getMinTerm() != null
                                && targetPeriodMonths >= rate.getMinTerm()
                                && (rate.getMaxTerm() == null
                                || targetPeriodMonths <= rate.getMaxTerm()))
                .findFirst();
    }

    private record DepositCandidate(
            DepositProduct product,
            DepositRate rate) {
    }
}