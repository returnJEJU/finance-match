package com.financematch.report.mapper;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@link ReportMapper}의 조인 쿼리 결과를 그대로 받는 내부 전송용 클래스.
 *
 * <p>API 응답 모양({@code scoreAxes} 배열 등)과 다르므로, {@code ReportService}가 이 값을 응답
 * DTO({@code ReportResponse})로 조립한다.
 */
@Getter
@NoArgsConstructor
public class ReportRow {

    private String memberName;
    private String partnerName;

    private BigDecimal totalScore;

    private BigDecimal assetStabilityScore;
    private String assetStabilityReason;

    private BigDecimal debtRepaymentScore;
    private String debtRepaymentReason;

    private BigDecimal financialValueScore;
    private String financialValueReason;

    private BigDecimal goalFeasibilityScore;
    private String goalFeasibilityReason;

    private BigDecimal taxStrategyScore;
    private String taxStrategyReason;

    private Integer targetMonths;
    private String loanPurpose;

    private String investmentProfileMe;
    private String investmentProfileYou;
    private String investmentProfileWe;
}
