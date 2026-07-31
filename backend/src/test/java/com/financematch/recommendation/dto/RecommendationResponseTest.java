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
                new PackageProductResponse(
                        101L,
                        "KB Star 정기예금",
                        "목돈을 안정적으로 운용하는 정기예금",
                        "https://obank.kbstar.com/deposit",
                        "기본금리",
                        "연 2.80%",
                        null,
                        null,
                        null,
                        null);
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
                                        301L,
                                        "KB증권 중개형 ISA",
                                        "직접 운용하는 절세 계좌",
                                        "https://www.kbsec.com/isa",
                                        TaxAccountType.ISA)));
        PersonalInvestmentRecommendationResponse investment =
                new PersonalInvestmentRecommendationResponse(
                        1L,
                        "홍길동",
                        List.of(
                                new PersonalInvestmentProductResponse(
                                        401L,
                                        "RISE 미국S&P500 ETF",
                                        "미국 대표 기업에 분산 투자하는 상품",
                                        "https://www.riseetf.co.kr/product",
                                        3,
                                        "고위험",
                                        28049)));
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
                "고위험",
                json.get("personalInvestmentRecommendation")
                        .get("products")
                        .get(0)
                        .get("riskLabel")
                        .textValue());
        assertEquals(
                "https://obank.kbstar.com/deposit",
                json.get("packageSlots")
                        .get(0)
                        .get("products")
                        .get(0)
                        .get("productUrl")
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
                new PackageProductResponse(
                        101L,
                        "KB Star 정기예금",
                        null,
                        "https://obank.kbstar.com/deposit",
                        "기본금리",
                        "연 2.80%",
                        null,
                        null,
                        null,
                        null);

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

    @Test
    void allowsInvestmentProductWithoutComparisonValue() {
        PackageProductResponse investment =
                new PackageProductResponse(
                        401L,
                        "RISE 미국S&P500 ETF",
                        "미국 대표 기업에 분산 투자하는 상품",
                        "https://www.riseetf.co.kr/product",
                        null,
                        null,
                        5,
                        "중립",
                        null,
                        3144);

        JsonNode json = objectMapper.valueToTree(investment);

        assertEquals(true, json.get("comparisonLabel").isNull());
        assertEquals(5, json.get("riskLevel").intValue());
        assertEquals("중립", json.get("riskLabel").textValue());
    }

    @Test
    void rejectsMismatchedRiskLabel() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new PersonalInvestmentProductResponse(
                                401L,
                                "RISE 미국S&P500 ETF",
                                null,
                                "https://www.riseetf.co.kr/product",
                                4,
                                "중립",
                                100));
    }
}
