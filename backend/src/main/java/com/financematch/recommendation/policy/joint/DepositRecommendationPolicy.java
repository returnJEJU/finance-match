package com.financematch.recommendation.policy.joint;

import com.financematch.product.domain.DepositRate;
import com.financematch.product.dto.DepositProduct;
import com.financematch.product.mapper.DepositMapper;
import com.financematch.product.type.DepositType;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.type.RecommendationReasonCode;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DepositRecommendationPolicy
        implements JointRecommendationPolicy {

    private static final BigDecimal MIN_DEPOSIT_AVAILABLE_BALANCE =
            BigDecimal.valueOf(9_100_000L);

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

        if (!isSlotOn(context)) {
            return List.of();
        }

        Integer targetPeriodMonths = context.getTargetPeriodMonths();

        if (targetPeriodMonths == null || targetPeriodMonths <= 0) {
            return List.of();
        }

        List<DepositProduct> candidates =
                depositMapper.findByType(DepositType.DEPOSIT);

        List<DepositCandidate> eligibleCandidates =
                candidates.stream()
                        .filter(product ->
                                supportsTerm(
                                        product,
                                        targetPeriodMonths))
                        .map(product ->
                                createCandidate(
                                        product,
                                        targetPeriodMonths))
                        .flatMap(Optional::stream)
                        .sorted(
                                Comparator.comparingInt(
                                                DepositCandidate::applicableTerm)
                                        .reversed()
                                        .thenComparing(
                                                (DepositCandidate candidate) ->
                                                        candidate.rate().getBaseRate(),
                                                Comparator.reverseOrder())
                                        .thenComparing(
                                                candidate ->
                                                        candidate.product().getProductId()))
                        .toList();

        return toRecommendations(eligibleCandidates);
    }

    private boolean isSlotOn(RecommendationContext context) {
        return context.getAvailableBalance() != null
                && context.getAvailableBalance()
                .compareTo(MIN_DEPOSIT_AVAILABLE_BALANCE) >= 0;
    }

    private List<RecommendedProduct> toRecommendations(
            List<DepositCandidate> candidates) {

        List<RecommendedProduct> recommendations = new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {
            DepositCandidate candidate = candidates.get(i);

            int rank = i + 1;
            boolean selected = rank == 1;

            recommendations.add(
                    new RecommendedProduct(
                            candidate.product().getProductId(),
                            rank,
                            selected,
                            selected ? RecommendationReasonCode.DEPOSIT_TERM_AND_RATE : null
                    )
            );
        }

        return List.copyOf(recommendations);
    }

    private boolean supportsTerm(
            DepositProduct product,
            int targetPeriodMonths) {

        if (product.getMinTerm() == null) {
            return false;
        }

        return targetPeriodMonths >= product.getMinTerm();
    }

    private Optional<DepositCandidate> createCandidate(
            DepositProduct product,
            int targetPeriodMonths) {

        Integer applicableTerm = findApplicableTerm(
                product,
                targetPeriodMonths);

        return findApplicableRate(product, applicableTerm)
                .map(rate ->
                        new DepositCandidate(
                                product,
                                rate,
                                applicableTerm));
    }

    private Integer findApplicableTerm(
            DepositProduct product,
            int targetPeriodMonths) {

        if (product.getMaxTerm() == null) {
            return targetPeriodMonths;
        }

        return Math.min(product.getMaxTerm(), targetPeriodMonths);
    }

    private Optional<DepositRate> findApplicableRate(
            DepositProduct product,
            int applicableTerm) {

        if (product.getRates() == null || product.getRates().isEmpty()) {
            return Optional.empty();
        }

        return product.getRates().stream()
                .filter(rate ->
                        rate.getMinTerm() != null
                                && rate.getBaseRate() != null
                                && applicableTerm >= rate.getMinTerm()
                                && (rate.getMaxTerm() == null
                                || applicableTerm <= rate.getMaxTerm()))
                .max(
                        Comparator.comparing(DepositRate::getMinTerm)
                                .thenComparing(DepositRate::getId));
    }

    private record DepositCandidate(
            DepositProduct product,
            DepositRate rate,
            int applicableTerm) {
    }
}
