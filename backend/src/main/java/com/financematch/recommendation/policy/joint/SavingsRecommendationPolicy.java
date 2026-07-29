package com.financematch.recommendation.policy.joint;

import com.financematch.product.domain.DepositRate;
import com.financematch.product.dto.DepositProduct;
import com.financematch.product.mapper.DepositMapper;
import com.financematch.product.type.DepositType;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.JointRecommendationPolicy;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;

@Component
public class SavingsRecommendationPolicy
        implements JointRecommendationPolicy {

    private final DepositMapper depositMapper;

    public SavingsRecommendationPolicy(
            DepositMapper depositMapper) {
        this.depositMapper = depositMapper;
    }

    @Override
    public RecommendationSlotType slotType() {
        return RecommendationSlotType.SAVINGS;
    }

    @Override
    public List<RecommendedProduct> recommend(
            RecommendationContext context) {

        List<DepositProduct> candidates =
                depositMapper.findByType(DepositType.SAVINGS);

        Integer targetPeriodMonths =
                context.getTargetPeriodMonths();

        if (targetPeriodMonths == null
                || targetPeriodMonths <= 0) {
            return List.of();
        }
        BigDecimal monthlyAvailableAmount =
                calculateCoupleMonthlyAvailableAmount(context);

        if (monthlyAvailableAmount == null
                || monthlyAvailableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        List<SavingsCandidate> eligibleCandidates =
                candidates.stream()
                        .filter(product ->
                                supportsTerm(
                                        product,
                                        targetPeriodMonths))
                        .filter(product ->
                                supportsSavingAmount(
                                        product,
                                        monthlyAvailableAmount))
                        .map(product ->
                                findApplicableRate(
                                        product,
                                        targetPeriodMonths)
                                        .map(rate ->
                                                new SavingsCandidate(
                                                        product,
                                                        rate)))
                        .flatMap(Optional::stream)
                        .filter(candidate ->
                                candidate.rate().getBaseRate() != null)
                        .sorted(
                                Comparator.comparing(
                                                (SavingsCandidate candidate) ->
                                                        candidate.rate().getBaseRate())
                                        .reversed()
                                        .thenComparing(
                                                candidate ->
                                                        candidate.product().getProductId()))
                        .toList();

        List<RecommendedProduct> recommendations =
                new ArrayList<>();

        int limit = Math.min(3, eligibleCandidates.size());

        for (int i = 0; i < limit; i++) {

            SavingsCandidate candidate =
                    eligibleCandidates.get(i);

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

        if (product.getRates() == null
                || product.getRates().isEmpty()) {
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

    private record SavingsCandidate(
            DepositProduct product,
            DepositRate rate) {
    }

    private BigDecimal calculateCoupleMonthlyAvailableAmount(
            RecommendationContext context) {

        BigDecimal inviterAmount =
                context.getInviterMonthlyAvailableAmount();

        BigDecimal inviteeAmount =
                context.getInviteeMonthlyAvailableAmount();

        if (inviterAmount == null || inviteeAmount == null) {
            return null;
        }

        return inviterAmount.add(inviteeAmount);
    }

    private boolean supportsSavingAmount(
            DepositProduct product,
            BigDecimal monthlyAvailableAmount) {

        Integer savingMin = product.getSavingMin();

        if (savingMin == null) {
            return true;
        }

        return monthlyAvailableAmount.compareTo(
                BigDecimal.valueOf(savingMin)) >= 0;
    }
}