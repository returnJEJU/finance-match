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
public class SavingsRecommendationPolicy
        implements JointRecommendationPolicy {

    private static final String KB_STAR_SAVINGS_NAME = "KB스타적금Ⅲ";
    private static final String GENERAL_E_PLUS_SAVINGS_NAME = "KB일반 e-plus정기적금";
    private static final BigDecimal E_PLUS_SPLIT_AMOUNT =
            BigDecimal.valueOf(1_000_000L);

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

        List<DepositProduct> candidates =
                depositMapper.findByType(DepositType.SAVINGS);

        boolean kbStarSavingsEligible =
                context.isInviterKbStarSavingsEligible()
                        || context.isInviteeKbStarSavingsEligible();

        List<SavingsCandidate> eligibleCandidates =
                candidates.stream()
                        .filter(product ->
                                supportsTerm(
                                        product,
                                        targetPeriodMonths))
                        .filter(product ->
                                supportsKbStarSavings(
                                        product,
                                        kbStarSavingsEligible))
                        .filter(product ->
                                supportsEPlusSavings(
                                        product,
                                        monthlyAvailableAmount))
                        .filter(product ->
                                supportsSavingAmount(
                                        product,
                                        monthlyAvailableAmount))
                        .map(product ->
                                createCandidate(
                                        product,
                                        targetPeriodMonths))
                        .flatMap(Optional::stream)
                        .sorted(
                                Comparator.comparing(
                                                (SavingsCandidate candidate) ->
                                                        isKbStarSavings(candidate.product()))
                                        .reversed()
                                        .thenComparing(
                                                Comparator.comparingInt(
                                                                SavingsCandidate::applicableTerm)
                                                        .reversed())
                                        .thenComparing(
                                                (SavingsCandidate candidate) ->
                                                        candidate.rate().getBaseRate(),
                                                Comparator.reverseOrder())
                                        .thenComparing(
                                                candidate ->
                                                        candidate.product().getProductId()))
                        .toList();

        return toRecommendations(eligibleCandidates);
    }

    private List<RecommendedProduct> toRecommendations(
            List<SavingsCandidate> candidates) {

        List<RecommendedProduct> recommendations =
                new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {

            SavingsCandidate candidate =
                    candidates.get(i);

            int rank = i + 1;
            boolean selected = rank == 1;

            recommendations.add(
                    new RecommendedProduct(
                            candidate.product().getProductId(),
                            rank,
                            selected,
                            selected
                                    ? (isKbStarSavings(candidate.product())
                                            ? RecommendationReasonCode.SAVINGS_KB_STAR_PRIORITY
                                            : RecommendationReasonCode.SAVINGS_TERM_AND_RATE)
                                    : null
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

    private boolean supportsKbStarSavings(
            DepositProduct product,
            boolean kbStarSavingsEligible) {

        return !isKbStarSavings(product) || kbStarSavingsEligible;
    }

    private boolean supportsEPlusSavings(
            DepositProduct product,
            BigDecimal monthlyAvailableAmount) {

        if (isGeneralEPlusSavings(product)) {
            return monthlyAvailableAmount.compareTo(E_PLUS_SPLIT_AMOUNT) > 0;
        }

        return true;
    }

    private Optional<SavingsCandidate> createCandidate(
            DepositProduct product,
            int targetPeriodMonths) {

        Integer applicableTerm = findApplicableTerm(
                product,
                targetPeriodMonths);

        if (applicableTerm == null) {
            return Optional.empty();
        }

        return findApplicableRate(product, applicableTerm)
                .map(rate ->
                        new SavingsCandidate(
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

        if (product.getRates() == null
                || product.getRates().isEmpty()) {
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

    private boolean isKbStarSavings(DepositProduct product) {
        return KB_STAR_SAVINGS_NAME.equals(product.getProductName());
    }

    private boolean isGeneralEPlusSavings(DepositProduct product) {
        return GENERAL_E_PLUS_SAVINGS_NAME.equals(product.getProductName());
    }

    private record SavingsCandidate(
            DepositProduct product,
            DepositRate rate,
            int applicableTerm) {
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
