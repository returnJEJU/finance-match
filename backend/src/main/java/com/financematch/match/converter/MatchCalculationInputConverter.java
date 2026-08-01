package com.financematch.match.converter;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Component;

import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MemberCalculationInput;
import com.financematch.match.domain.MatchCoupleData;
import com.financematch.match.domain.MatchMemberData;

@Component
public class MatchCalculationInputConverter {

    public MatchCalculationInput convert(
            MatchCoupleData couple,
            MatchMemberData memberA,
            MatchMemberData memberB
    ) {
        if (couple == null || memberA == null || memberB == null) {
            throw new IllegalArgumentException("금융 궁합도 계산 데이터가 필요합니다.");
        }

        return MatchCalculationInput.builder()
                .memberA(convertMember(memberA))
                .memberB(convertMember(memberB))
                .firstGoalType(couple.getFirstGoalType())
                .targetAmount(couple.getTargetAmount())
                .targetPeriodMonths(couple.getTargetPeriodMonths())
                .build();
    }

    private MemberCalculationInput convertMember(MatchMemberData member) {
        if (member.getBirthDate() == null) {
            throw new IllegalArgumentException("회원 생년월일이 필요합니다.");
        }

        int age = Period.between(
                member.getBirthDate(),
                LocalDate.now()
        ).getYears();

        return MemberCalculationInput.builder()
                .age(age)
                .ageGroupAssetMedian(member.getAgeGroupAssetMedian())
                .financialAsset(member.getFinancialAsset())

                .annualIncome(member.getAnnualIncome())
                .totalDebt(member.getTotalDebt())
                .annualDebtPayment(member.getAnnualDebtPayment())

                .financialAssetRatioScore(
                        convertFinancialAssetRatio(member.getFinancialAssetRatio())
                )
                .investmentExperienceScore(
                        member.getInvestmentExperienceScore()
                )
                .financialKnowledgeScore(
                        convertFinancialKnowledge(member.getFinancialKnowledge())
                )
                .capitalPreservationScore(
                        convertCapitalPreservation(
                                member.getCapitalPreservationAttitude()
                        )
                )

                .monthlyAvailableAmount(member.getMonthlyAvailableAmount())
                .pensionSavingBalance(member.getPensionSavingBalance())
                .irpBalance(member.getIrpBalance())
                .pensionAnnualPayment(member.getPensionAnnualPayment())
                .irpAnnualPayment(member.getIrpAnnualPayment())

                .dcAnnualPayment(member.getDcAnnualPayment())
                .isaAnnualDeposit(member.getIsaAnnualDeposit())
                .taxEligibilityStatus(member.getTaxEligibilityStatus())
                .isaEligibilityStatus(member.getIsaEligibilityStatus())
                .hasIsa(member.isHasIsa())
                .hasIrp(member.isHasIrp())
                .hasPensionSaving(member.isHasPensionSaving())
                .build();
    }

    private int convertFinancialAssetRatio(String value) {
        if (value == null) {
            throw new IllegalArgumentException("금융자산 비중 응답이 필요합니다.");
        }

        return switch (value) {
            case "UNDER_10" -> 1;
            case "UNDER_30" -> 2;
            case "UNDER_50" -> 3;
            case "UNDER_80" -> 4;
            case "OVER_80" -> 5;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 금융자산 비중 코드입니다: " + value
            );
        };
    }

    private int convertFinancialKnowledge(String value) {
        if (value == null) {
            throw new IllegalArgumentException("금융 지식 응답이 필요합니다.");
        }

        return switch (value) {
            case "VERY_LOW" -> 1;
            case "LOW" -> 2;
            case "MEDIUM" -> 3;
            case "HIGH" -> 4;
            case "VERY_HIGH" -> 5;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 금융 지식 코드입니다: " + value
            );
        };
    }

    private int convertCapitalPreservation(String value) {
        if (value == null) {
            throw new IllegalArgumentException("원금 보존 응답이 필요합니다.");
        }

        return switch (value) {
            case "ZERO" -> 1;
            case "UNDER_10" -> 2;
            case "UNDER_20" -> 3;
            case "UNDER_50" -> 4;
            case "UNDER_70" -> 5;
            case "FULL" -> 6;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 원금 보존 코드입니다: " + value
            );
        };
    }
}