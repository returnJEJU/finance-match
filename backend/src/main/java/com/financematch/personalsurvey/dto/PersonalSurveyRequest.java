package com.financematch.personalsurvey.dto;

import com.financematch.personalsurvey.domain.CapitalPreservationAttitude;
import com.financematch.personalsurvey.domain.FinancialAssetRatio;
import com.financematch.personalsurvey.domain.FinancialKnowledge;
import com.financematch.personalsurvey.domain.InvestmentExperience;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Set;

@Getter
@NoArgsConstructor
public class PersonalSurveyRequest {

    @NotNull(message = "연소득을 입력해 주세요.")
    @PositiveOrZero(message = "연소득은 0원 이상이어야 합니다.")
    @Digits(integer = 15, fraction = 0, message = "연소득은 원 단위 정수로 입력해 주세요.")
    private BigDecimal annualIncome;

    @NotNull(message = "월 저축·투자 가능액을 입력해 주세요.")
    @PositiveOrZero(message = "월 저축·투자 가능액은 0원 이상이어야 합니다.")
    @Digits(integer = 15, fraction = 0, message = "월 저축·투자 가능액은 원 단위 정수로 입력해 주세요.")
    private BigDecimal monthlyAvailableAmount;

    @NotNull(message = "금융자산 비중을 선택해 주세요.")
    private FinancialAssetRatio financialAssetRatio;

    @NotEmpty(message = "투자 경험을 하나 이상 선택해 주세요.")
    @Size(max = 5, message = "투자 경험 선택값이 올바르지 않습니다.")
    private Set<InvestmentExperience> investmentExperiences;

    @NotNull(message = "금융상품 이해도를 선택해 주세요.")
    private FinancialKnowledge financialKnowledge;

    @NotNull(message = "원금보존 태도를 선택해 주세요.")
    private CapitalPreservationAttitude capitalPreservationAttitude;
}
