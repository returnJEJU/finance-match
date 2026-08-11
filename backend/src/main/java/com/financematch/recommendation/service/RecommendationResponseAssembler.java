package com.financematch.recommendation.service;

import com.financematch.product.type.LoanPurpose;
import com.financematch.recommendation.domain.JointRecommendationProduct;
import com.financematch.recommendation.domain.PersonalInvestmentProduct;
import com.financematch.recommendation.domain.PersonalTaxSavingProduct;
import com.financematch.recommendation.domain.RecommendationResult;
import com.financematch.recommendation.dto.PackageProductResponse;
import com.financematch.recommendation.dto.PackageSlotResponse;
import com.financematch.recommendation.dto.PersonalInvestmentProductResponse;
import com.financematch.recommendation.dto.PersonalInvestmentRecommendationResponse;
import com.financematch.recommendation.dto.PersonalTaxSavingRecommendationResponse;
import com.financematch.recommendation.dto.RecommendationResponse;
import com.financematch.recommendation.dto.TaxSavingProductResponse;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RecommendationResponseAssembler {

    public RecommendationResponse assemble(
            RecommendationResult result,
            List<JointRecommendationProduct> jointProducts,
            List<PersonalTaxSavingProduct> taxSavingProducts,
            List<PersonalInvestmentProduct> investmentProducts) {

        if (result == null) {
            throw new IllegalArgumentException("추천 조회 결과는 필수입니다.");
        }

        return new RecommendationResponse(
                result.getRecommendationId(),
                packageName(result.getFirstGoalType()),
                result.isHasHighInterestDebt(),
                assemblePackageSlots(jointProducts),
                assembleTaxSaving(taxSavingProducts),
                assembleInvestment(investmentProducts));
    }

    private List<PackageSlotResponse> assemblePackageSlots(
            List<JointRecommendationProduct> products) {

        Map<Long, List<JointRecommendationProduct>> productsBySlot = new LinkedHashMap<>();
        products.forEach(
                product ->
                        productsBySlot
                                .computeIfAbsent(product.getSlotId(), ignored -> new ArrayList<>())
                                .add(product));

        return productsBySlot.values().stream()
                .filter(slotProducts -> !slotProducts.isEmpty())
                .map(this::assemblePackageSlot)
                .toList();
    }

    private PackageSlotResponse assemblePackageSlot(
            List<JointRecommendationProduct> slotProducts) {

        JointRecommendationProduct first = slotProducts.get(0);
        Long selectedProductId =
                slotProducts.stream()
                        .filter(JointRecommendationProduct::isSelected)
                        .map(JointRecommendationProduct::getProductId)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "공동 추천 슬롯의 대표 상품이 없습니다: "
                                                        + first.getSlotType()));

        List<PackageProductResponse> products =
                slotProducts.stream()
                        .map(this::assemblePackageProduct)
                        .toList();

        return new PackageSlotResponse(
                first.getSlotId(),
                selectedProductId,
                first.getSlotType(),
                slotName(first.getSlotType()),
                products);
    }

    private PackageProductResponse assemblePackageProduct(
            JointRecommendationProduct product) {

        RecommendationSlotType slotType = product.getSlotType();
        String comparisonLabel = null;
        String comparisonValue = null;
        Integer riskLevel = null;
        String riskLabel = null;
        Integer aum = null;
        LoanPurpose loanPurpose = null;

        switch (slotType) {
            case DEPOSIT, SAVINGS -> {
                comparisonLabel = "기본금리";
                comparisonValue = formatRate(product.getApplicableBaseRate());
            }
            case INVESTMENT -> {
                riskLevel = product.getRiskLevel();
                riskLabel = riskLabel(riskLevel);
                aum = product.getAum();
            }
            case LOAN -> {
                comparisonLabel = "최고금리";
                comparisonValue = formatRate(product.getLoanMaxRate());
                loanPurpose = product.getLoanPurpose();
            }
        }

        return new PackageProductResponse(
                product.getProductId(),
                product.getProductName(),
                product.getDescription(),
                product.getProductUrl(),
                comparisonLabel,
                comparisonValue,
                riskLevel,
                riskLabel,
                loanPurpose,
                aum);
    }

    private PersonalTaxSavingRecommendationResponse assembleTaxSaving(
            List<PersonalTaxSavingProduct> products) {

        if (products.isEmpty()) {
            return null;
        }

        PersonalTaxSavingProduct first = products.get(0);
        List<TaxSavingProductResponse> responses =
                products.stream()
                        .map(
                                product ->
                                        new TaxSavingProductResponse(
                                                product.getProductId(),
                                                product.getProductName(),
                                                product.getDescription(),
                                                product.getProductUrl(),
                                                product.getAccountType()))
                        .toList();

        return new PersonalTaxSavingRecommendationResponse(
                first.getTargetMemberId(),
                first.getTargetMemberName(),
                responses);
    }

    private PersonalInvestmentRecommendationResponse assembleInvestment(
            List<PersonalInvestmentProduct> products) {

        if (products.isEmpty()) {
            return null;
        }

        PersonalInvestmentProduct first = products.get(0);
        List<PersonalInvestmentProductResponse> responses =
                products.stream()
                        .map(
                                product ->
                                        new PersonalInvestmentProductResponse(
                                                product.getProductId(),
                                                product.getProductName(),
                                                product.getDescription(),
                                                product.getProductUrl(),
                                                product.getRiskLevel(),
                                                riskLabel(product.getRiskLevel()),
                                                product.getAum()))
                        .toList();

        return new PersonalInvestmentRecommendationResponse(
                first.getTargetMemberId(),
                first.getTargetMemberName(),
                responses);
    }

    private String packageName(String firstGoalType) {
        if (firstGoalType == null || firstGoalType.isBlank()) {
            throw new IllegalStateException("추천 패키지명을 만들 1순위 목표가 없습니다.");
        }

        String goalLabel =
                switch (firstGoalType) {
                    case "MARRIAGE" -> "신혼집";
                    case "HOUSING" -> "내 집 마련";
                    case "RETIREMENT" -> "노후 준비";
                    case "INVESTMENT" -> "여유자금";
                    case "SHORT_TERM" -> "목돈 굴리기";
                    default ->
                            throw new IllegalStateException(
                                    "지원하지 않는 1순위 목표입니다: " + firstGoalType);
                };

        return goalLabel + " 스타터 패키지";
    }

    private String slotName(RecommendationSlotType slotType) {
        return switch (slotType) {
            case DEPOSIT -> "예금";
            case SAVINGS -> "적금";
            case INVESTMENT -> "투자";
            case LOAN -> "대출";
        };
    }

    private String formatRate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalStateException("추천 상품의 금리 정보가 없습니다.");
        }
        return "연 " + rate.toPlainString() + "%";
    }

    private String riskLabel(Integer riskLevel) {
        if (riskLevel == null) {
            throw new IllegalStateException("투자 추천 상품의 위험등급이 없습니다.");
        }
        return switch (riskLevel) {
            case 1, 2 -> "초고위험";
            case 3 -> "고위험";
            case 4 -> "위험";
            case 5 -> "중립";
            case 6 -> "안정";
            default -> throw new IllegalStateException("지원하지 않는 상품 위험등급입니다: " + riskLevel);
        };
    }
}
