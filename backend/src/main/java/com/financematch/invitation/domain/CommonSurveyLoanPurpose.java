package com.financematch.invitation.domain;

public enum CommonSurveyLoanPurpose {
    // product.type.LoanPurpose와 MyBatis 타입 별칭이 충돌하는 문제로 인해 enum 명칭을 CommonSurveyLoanPurpose으로 변경함
    NONE,
    JEONSE,
    HOUSING,
    CAR,
    BUSINESS
}
