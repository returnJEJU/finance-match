package com.financematch.match.domain;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchCoupleData {

    private Long coupleId;
    private Long inviterId;
    private Long inviteeId;

    private String firstGoalType;
    private BigDecimal targetAmount;
    private int targetPeriodMonths;
}