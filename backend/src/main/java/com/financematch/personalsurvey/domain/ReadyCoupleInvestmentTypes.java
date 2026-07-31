package com.financematch.personalsurvey.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReadyCoupleInvestmentTypes {

    private Long coupleId;
    private PersonalInvestmentType inviterInvestmentType;
    private PersonalInvestmentType inviteeInvestmentType;
}
