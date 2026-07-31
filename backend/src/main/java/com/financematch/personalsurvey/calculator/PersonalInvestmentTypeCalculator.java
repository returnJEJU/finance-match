package com.financematch.personalsurvey.calculator;

import com.financematch.invitation.domain.GoalType;
import com.financematch.personalsurvey.domain.CapitalPreservationAttitude;
import com.financematch.personalsurvey.domain.InvestmentExperience;
import com.financematch.personalsurvey.domain.PersonalInvestmentType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;


@Component
public class PersonalInvestmentTypeCalculator {

    private static final double MAX_SCORE = 44.5;
    private static final double PERCENTAGE_SCALE = 100.0;

    public PersonalInvestmentType calculate(
            PersonalInvestmentCalculationInput input,
            LocalDate assessmentDate) {

        double rawScore = ageScore(input.getBirthDate(), assessmentDate)
                        + incomeScore(input.getAnnualIncome())
                        + input.getFinancialAssetRatio().getScore()
                        + investmentExperienceScore(input)
                        + input.getFinancialKnowledge().getScore()
                        + input.getCapitalPreservationAttitude().getScore()
                        + goalScore(input.getFirstGoalType())
                        + periodScore(input.getTargetPeriodMonths());

        double convertedScore = rawScore / MAX_SCORE * PERCENTAGE_SCALE;

        PersonalInvestmentType calculated = classify(convertedScore);

        // 원금보존 추구는 환산점수와 관계없이 안정형
        if (input.getCapitalPreservationAttitude() == CapitalPreservationAttitude.ZERO) {
            return PersonalInvestmentType.STABLE;
        }

        // 단기운용 목적 또는 10% 이내 손실 감내는 위험중립형 이하
        if (input.getFirstGoalType() == GoalType.SHORT_TERM
                || input.getCapitalPreservationAttitude() == CapitalPreservationAttitude.UNDER_10) {
            return calculated.capAt(PersonalInvestmentType.NEUTRAL);
        }

        return calculated;
    }

    private PersonalInvestmentType classify(double score) {
        if (score <= 43.0) {
            return PersonalInvestmentType.STABLE;
        } else if (score <= 55.0) {
            return PersonalInvestmentType.STABLE_SEEKING;
        } else if (score <= 68.0) {
            return PersonalInvestmentType.NEUTRAL;
        } else if (score <= 81.0) {
            return PersonalInvestmentType.AGGRESSIVE;
        } else {
            return PersonalInvestmentType.VERY_AGGRESSIVE;
        }
    }

    private double ageScore(LocalDate birthDate, LocalDate assessmentDate) {

        int age = Period.between(birthDate, assessmentDate).getYears(); // 만 나이

        if (age <= 19) {
            return 0.5;
        } else if (age <= 39) {
            return 2.5; // 20~30대
        } else if (age <= 49) {
            return 2.0;
        } else if (age <= 59) {
            return 1.5;
        } else {
            return 0.5; // 60대 이상
        }

    }

    private double incomeScore(long annualIncome) {
        if (annualIncome <= 30_000_000L) {
            return 6.0;
        } else if (annualIncome <= 50_000_000L) {
            return 6.5;
        } else if (annualIncome <= 100_000_000L) {
            return 7.0;
        } else if (annualIncome <= 300_000_000L) {
            return 7.5;
        } else {
            return 8.0;
        }
    }

    private double investmentExperienceScore(PersonalInvestmentCalculationInput input) {

        return input.getInvestmentExperiences()
                .stream()
                .mapToDouble(InvestmentExperience::getScore)
                .max()
                .orElseThrow(() -> new IllegalArgumentException("투자 경험이 필요합니다."));
    }

    private double goalScore(GoalType goalType) {
        return switch (goalType) {
            case INVESTMENT -> 4.0;
            case RETIREMENT -> 3.0;
            case MARRIAGE, HOUSING -> 2.0;
            case SHORT_TERM -> 1.0;
        };
    }

    private double periodScore(int months) {
        if (months < 6) {
            return 0.5;
        } else if (months < 12) {
            return 1.0;
        } else if (months < 36) {
            return 2.0;
        } else {
            return 2.5;
        }
    }

}
