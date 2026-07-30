package com.financematch.recommendation.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.product.type.TaxAccountType;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesApiContractFieldNames() {
        PackageProductResponse packageProduct =
                new PackageProductResponse(101L, "KB Star 정기예금", "기본금리", "연 2.80%");
        PackageSlotResponse slot =
                new PackageSlotResponse(
                        11L,
                        101L,
                        RecommendationSlotType.DEPOSIT,
                        "예금",
                        List.of(packageProduct));
        PersonalTaxSavingRecommendationResponse taxSaving =
                new PersonalTaxSavingRecommendationResponse(
                        1L,
                        "홍길동",
                        List.of(
                                new TaxSavingProductResponse(
                                        301L, "KB증권 중개형 ISA", TaxAccountType.ISA)));
        PersonalInvestmentRecommendationResponse investment =
                new PersonalInvestmentRecommendationResponse(
                        1L,
                        "홍길동",
                        List.of(
                                new PersonalInvestmentProductResponse(
                                        401L, "RISE 미국S&P500 ETF", "AGGRESSIVE")));
        RecommendationResponse response =
                new RecommendationResponse(
                        100L, false, List.of(slot), taxSaving, investment);

        JsonNode json = objectMapper.valueToTree(response);

        assertEquals(100L, json.get("recommendationId").longValue());
        assertEquals(false, json.get("hasHighInterestDebt").booleanValue());
        assertEquals(101L, json.get("packageSlots").get(0).get("selectedProductId").longValue());
        assertEquals(
                1L,
                json.get("personalTaxSavingRecommendation")
                        .get("targetMemberId")
                        .longValue());
        assertEquals(
                "AGGRESSIVE",
                json.get("personalInvestmentRecommendation")
                        .get("products")
                        .get(0)
                        .get("investmentType")
                        .textValue());
    }

    @Test
    void allowsNullPersonalRecommendations() {
        RecommendationResponse response =
                new RecommendationResponse(100L, true, List.of(), null, null);

        JsonNode json = objectMapper.valueToTree(response);

        assertEquals(0, json.get("packageSlots").size());
        assertEquals(true, json.get("personalTaxSavingRecommendation").isNull());
        assertEquals(true, json.get("personalInvestmentRecommendation").isNull());
    }

    @Test
    void rejectsActiveSlotWithoutProducts() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PackageSlotResponse(
                                11L,
                                101L,
                                RecommendationSlotType.DEPOSIT,
                                "예금",
                                List.of()));
    }

    @Test
    void rejectsSelectedProductOutsideSlotProducts() {
        PackageProductResponse product =
                new PackageProductResponse(101L, "KB Star 정기예금", "기본금리", "연 2.80%");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PackageSlotResponse(
                                11L,
                                999L,
                                RecommendationSlotType.DEPOSIT,
                                "예금",
                                List.of(product)));
    }

    @Test
    void rejectsEmptyPersonalRecommendationObject() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PersonalInvestmentRecommendationResponse(1L, "홍길동", List.of()));
    }
}
