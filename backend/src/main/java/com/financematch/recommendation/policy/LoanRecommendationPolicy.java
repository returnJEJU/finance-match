package com.financematch.recommendation.policy;

import com.financematch.product.dto.LoanProduct;
import com.financematch.product.mapper.LoanMapper;
import com.financematch.product.type.ApplicationChannel;
import com.financematch.product.type.IncomeBasis;
import com.financematch.product.type.LoanPurpose;
import com.financematch.product.type.LoanTargetGroup;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.policy.joint.JointRecommendationPolicy;
import com.financematch.recommendation.type.RecommendationReasonCode;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanRecommendationPolicy implements JointRecommendationPolicy {

    private final LoanMapper loanMapper;

    @Override
    public RecommendationSlotType slotType() {
        return RecommendationSlotType.LOAN;
    }

    @Override
    public List<RecommendedProduct> recommend(RecommendationContext context) {
        if (context.isHasHighRateDebt() || hasNoLoanPurpose(context.getLoanPurpose())) {
            return List.of();
        }

        LoanPurpose loanPurpose = parseLoanPurpose(context.getLoanPurpose());
        int inviterAge = age(context.getInviterBirthDate());
        int inviteeAge = age(context.getInviteeBirthDate());

        List<LoanProduct> rankedProducts =
                loanMapper.findByPurpose(loanPurpose).stream()
                        .filter(product -> ageEligible(product, inviterAge, inviteeAge))
                        .filter(product -> incomeEligible(product, context))
                        .sorted(
                                Comparator.comparingInt(
                                                (LoanProduct product) ->
                                                        targetGroupPriority(
                                                                product.getTargetGroup()))
                                        .thenComparing(
                                                LoanProduct::getMaxRate,
                                                Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparingInt(
                                                product ->
                                                        applicationChannelPriority(
                                                                product.getApplicationChannel()))
                                        .thenComparing(LoanProduct::getProductId))
                        .toList();

        return IntStream.range(0, rankedProducts.size())
                .mapToObj(
                        index ->
                                new RecommendedProduct(
                                        rankedProducts.get(index).getProductId(),
                                        index + 1,
                                        index == 0,
                                        index == 0
                                                ? RecommendationReasonCode.LOAN_TARGET_GROUP_RATE_AND_CHANNEL
                                                : null))
                .toList();
    }

    private boolean hasNoLoanPurpose(String loanPurpose) {
        return loanPurpose == null || loanPurpose.isBlank() || "NONE".equals(loanPurpose);
    }

    private LoanPurpose parseLoanPurpose(String loanPurpose) {
        try {
            return LoanPurpose.valueOf(loanPurpose);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 대출 목적입니다: " + loanPurpose, exception);
        }
    }

    private int age(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("생년월일은 필수입니다.");
        }
        return Period.between(birthDate, LocalDate.now(ZoneOffset.UTC)).getYears();
    }

    private boolean ageEligible(LoanProduct product, int inviterAge, int inviteeAge) {
        return ageEligible(product, inviterAge) || ageEligible(product, inviteeAge);
    }

    private boolean ageEligible(LoanProduct product, int age) {
        return (product.getMinAge() == null || age >= product.getMinAge())
                && (product.getMaxAge() == null || age <= product.getMaxAge());
    }

    private boolean incomeEligible(LoanProduct product, RecommendationContext context) {
        if (product.getMaxIncome() == null) {
            return true;
        }
        if (context.getInviterAnnualIncome() == null || context.getInviteeAnnualIncome() == null) {
            throw new IllegalArgumentException("연소득은 필수입니다.");
        }

        BigDecimal maxIncome = BigDecimal.valueOf(product.getMaxIncome());
        if (product.getIncomeBasis() == IncomeBasis.COUPLE) {
            BigDecimal coupleIncome =
                    context.getInviterAnnualIncome().add(context.getInviteeAnnualIncome());
            return coupleIncome.compareTo(maxIncome) <= 0;
        }
        if (product.getIncomeBasis() == IncomeBasis.INDIVIDUAL) {
            BigDecimal lowerIncome =
                    context.getInviterAnnualIncome().min(context.getInviteeAnnualIncome());
            return lowerIncome.compareTo(maxIncome) <= 0;
        }
        return false;
    }

    private int targetGroupPriority(LoanTargetGroup targetGroup) {
        if (targetGroup == null) {
            return Integer.MAX_VALUE;
        }
        return switch (targetGroup) {
            case NEWLYWED -> 0;
            case GENERAL -> 1;
            case OTHER -> 2;
        };
    }

    private int applicationChannelPriority(ApplicationChannel channel) {
        if (channel == null) {
            return Integer.MAX_VALUE;
        }
        return switch (channel) {
            case MOBILE -> 0;
            case ONLINE -> 1;
            case BRANCH -> 2;
        };
    }
}
