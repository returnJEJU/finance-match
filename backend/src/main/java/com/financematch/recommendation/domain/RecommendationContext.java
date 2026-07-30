package com.financematch.recommendation.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationContext {

    private Long coupleId;
    private Long inviterId;
    private Long inviteeId;

    private String inviterInvestmentType;
    private String inviteeInvestmentType;
    private LocalDate inviterBirthDate;
    private LocalDate inviteeBirthDate;
    private boolean inviterKbStarSavingsEligible;
    private boolean inviteeKbStarSavingsEligible;

    private BigDecimal inviterAnnualIncome;
    private BigDecimal inviteeAnnualIncome;
    private BigDecimal inviterMonthlyAvailableAmount;
    private BigDecimal inviteeMonthlyAvailableAmount;
    private String inviterFinancialKnowledge;
    private String inviteeFinancialKnowledge;

    private String firstGoalType;
    private String secondGoalType;
    private Integer targetPeriodMonths;
    private String loanPurpose;
    private boolean hasLoanWithinOneMonth;

    private BigDecimal availableBalance;
    private boolean hasHighRateDebt;

    private boolean inviterHasPensionSaving;
    private boolean inviterHasIrp;
    private boolean inviterHasIsa;
    private String inviterTaxEligibilityStatus;
    private String inviterIsaEligibilityStatus;
    private boolean inviteeHasPensionSaving;
    private boolean inviteeHasIrp;
    private boolean inviteeHasIsa;
    private String inviteeTaxEligibilityStatus;
    private String inviteeIsaEligibilityStatus;

    public List<Long> memberIds() {
        return List.of(inviterId, inviteeId);
    }
}
