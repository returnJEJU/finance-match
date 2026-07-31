package com.financematch.personalsurvey.calculator;

import com.financematch.personalsurvey.domain.CoupleInvestmentType;
import com.financematch.personalsurvey.domain.PersonalInvestmentType;
import org.springframework.stereotype.Component;

@Component
public class CoupleInvestmentTypeCalculator {

    public CoupleInvestmentType calculate(
            PersonalInvestmentType first,
            PersonalInvestmentType second) {

        int difference = Math.abs(first.getLevel() - second.getLevel());

        return CoupleInvestmentType.valueOf("DIFF_" + difference);
    }
}
