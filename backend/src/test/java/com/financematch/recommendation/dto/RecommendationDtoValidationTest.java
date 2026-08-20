package com.financematch.recommendation.dto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.product.type.TaxAccountType;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationDtoValidationTest {

    @Test
    void validatesPackageProductRequiredValues() {
        assertThrows(IllegalArgumentException.class, () -> comparisonProduct(null, "상품", null));
        assertThrows(IllegalArgumentException.class, () -> comparisonProduct(1L, null, null));
        assertThrows(IllegalArgumentException.class, () -> comparisonProduct(1L, " ", null));
        assertThrows(IllegalArgumentException.class, () -> comparisonProduct(1L, "상품", " "));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PackageProductResponse(
                                1L, "상품", null, null, " ", "기본금리", "연 3%", null, null, null, null));
    }

    @Test
    void validatesPackageProductComparisonPair() {
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct("기본금리", null, null, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct(null, "연 3%", null, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct(" ", "연 3%", null, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct("기본금리", " ", null, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct(null, null, null, null, null));
    }

    @Test
    void validatesPackageProductRiskPairAndLabels() {
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct(null, null, 4, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct(null, null, null, "위험", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct(null, null, 4, " ", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct("기본금리", "연 3%", 4, "위험", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct(null, null, 0, "위험", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct(null, null, 4, "중립", null));

        assertDoesNotThrow(() -> packageProduct(null, null, 1, "초고위험", 0));
        assertDoesNotThrow(() -> packageProduct(null, null, 2, "초고위험", 0));
        assertDoesNotThrow(() -> packageProduct(null, null, 3, "고위험", 0));
        assertDoesNotThrow(() -> packageProduct(null, null, 4, "위험", 0));
        assertDoesNotThrow(() -> packageProduct(null, null, 5, "중립", 0));
        assertDoesNotThrow(() -> packageProduct(null, null, 6, "안정", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> packageProduct(null, null, 6, "안정", -1));
    }

    @Test
    void validatesPackageSlot() {
        PackageProductResponse product = comparisonProduct(1L, "상품", null);

        assertThrows(
                IllegalArgumentException.class,
                () -> new PackageSlotResponse(null, 1L, RecommendationSlotType.DEPOSIT, "예금", List.of(product)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PackageSlotResponse(1L, null, RecommendationSlotType.DEPOSIT, "예금", List.of(product)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PackageSlotResponse(1L, 1L, null, "예금", List.of(product)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PackageSlotResponse(1L, 1L, RecommendationSlotType.DEPOSIT, null, List.of(product)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PackageSlotResponse(1L, 1L, RecommendationSlotType.DEPOSIT, " ", List.of(product)));
    }

    @Test
    void validatesPersonalInvestmentProduct() {
        assertThrows(IllegalArgumentException.class, () -> personalInvestment(null, "상품", null, "url", 4, "위험", 0));
        assertThrows(IllegalArgumentException.class, () -> personalInvestment(1L, null, null, "url", 4, "위험", 0));
        assertThrows(IllegalArgumentException.class, () -> personalInvestment(1L, " ", null, "url", 4, "위험", 0));
        assertThrows(IllegalArgumentException.class, () -> personalInvestment(1L, "상품", " ", "url", 4, "위험", 0));
        assertThrows(IllegalArgumentException.class, () -> personalInvestment(1L, "상품", null, null, 4, "위험", 0));
        assertThrows(IllegalArgumentException.class, () -> personalInvestment(1L, "상품", null, " ", 4, "위험", 0));
        assertThrows(IllegalArgumentException.class, () -> personalInvestment(1L, "상품", null, "url", 4, null, 0));
        assertThrows(IllegalArgumentException.class, () -> personalInvestment(1L, "상품", null, "url", 4, " ", 0));
        assertThrows(IllegalArgumentException.class, () -> personalInvestment(1L, "상품", null, "url", 4, "위험", -1));
        assertThrows(IllegalArgumentException.class, () -> personalInvestment(1L, "상품", null, "url", 0, "위험", 0));
        assertThrows(IllegalArgumentException.class, () -> personalInvestment(1L, "상품", null, "url", 4, "중립", 0));

        assertDoesNotThrow(() -> personalInvestment(1L, "상품", null, "url", 1, "초고위험", 0));
        assertDoesNotThrow(() -> personalInvestment(1L, "상품", null, "url", 2, "초고위험", 0));
        assertDoesNotThrow(() -> personalInvestment(1L, "상품", null, "url", 3, "고위험", 0));
        assertDoesNotThrow(() -> personalInvestment(1L, "상품", null, "url", 4, "위험", 0));
        assertDoesNotThrow(() -> personalInvestment(1L, "상품", null, "url", 5, "중립", 0));
        assertDoesNotThrow(() -> personalInvestment(1L, "상품", null, "url", 6, "안정", 0));
    }

    @Test
    void validatesPersonalRecommendationContainers() {
        PersonalInvestmentProductResponse investment = personalInvestment(1L, "상품", null, "url", 4, "위험", 0);
        TaxSavingProductResponse tax = taxProduct(1L, "절세", null, "url", TaxAccountType.ISA);

        assertThrows(IllegalArgumentException.class, () -> new PersonalInvestmentRecommendationResponse(null, "회원", List.of(investment)));
        assertThrows(IllegalArgumentException.class, () -> new PersonalInvestmentRecommendationResponse(1L, null, List.of(investment)));
        assertThrows(IllegalArgumentException.class, () -> new PersonalInvestmentRecommendationResponse(1L, " ", List.of(investment)));
        assertThrows(IllegalArgumentException.class, () -> new PersonalInvestmentRecommendationResponse(1L, "회원", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PersonalTaxSavingRecommendationResponse(null, "회원", List.of(tax)));
        assertThrows(IllegalArgumentException.class, () -> new PersonalTaxSavingRecommendationResponse(1L, null, List.of(tax)));
        assertThrows(IllegalArgumentException.class, () -> new PersonalTaxSavingRecommendationResponse(1L, " ", List.of(tax)));
        assertThrows(IllegalArgumentException.class, () -> new PersonalTaxSavingRecommendationResponse(1L, "회원", List.of()));
    }

    @Test
    void validatesTaxSavingProduct() {
        assertThrows(IllegalArgumentException.class, () -> taxProduct(null, "절세", null, "url", TaxAccountType.ISA));
        assertThrows(IllegalArgumentException.class, () -> taxProduct(1L, "절세", null, "url", null));
        assertThrows(IllegalArgumentException.class, () -> taxProduct(1L, null, null, "url", TaxAccountType.ISA));
        assertThrows(IllegalArgumentException.class, () -> taxProduct(1L, " ", null, "url", TaxAccountType.ISA));
        assertThrows(IllegalArgumentException.class, () -> taxProduct(1L, "절세", " ", "url", TaxAccountType.ISA));
        assertThrows(IllegalArgumentException.class, () -> taxProduct(1L, "절세", null, null, TaxAccountType.ISA));
        assertThrows(IllegalArgumentException.class, () -> taxProduct(1L, "절세", null, " ", TaxAccountType.ISA));
    }

    @Test
    void validatesRecommendationResponse() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecommendationResponse(null, "패키지", false, List.of(), null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecommendationResponse(1L, null, false, List.of(), null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecommendationResponse(1L, " ", false, List.of(), null, null));
    }

    private PackageProductResponse comparisonProduct(Long id, String name, String reason) {
        return new PackageProductResponse(
                id, name, null, reason, "url", "기본금리", "연 3%", null, null, null, null);
    }

    private PackageProductResponse packageProduct(
            String comparisonLabel,
            String comparisonValue,
            Integer riskLevel,
            String riskLabel,
            Integer aum) {
        return new PackageProductResponse(
                1L,
                "상품",
                null,
                null,
                "url",
                comparisonLabel,
                comparisonValue,
                riskLevel,
                riskLabel,
                null,
                aum);
    }

    private PersonalInvestmentProductResponse personalInvestment(
            Long id,
            String name,
            String reason,
            String url,
            int riskLevel,
            String riskLabel,
            int aum) {
        return new PersonalInvestmentProductResponse(
                id, name, null, reason, url, riskLevel, riskLabel, aum);
    }

    private TaxSavingProductResponse taxProduct(
            Long id,
            String name,
            String reason,
            String url,
            TaxAccountType accountType) {
        return new TaxSavingProductResponse(id, name, null, reason, url, accountType);
    }
}
