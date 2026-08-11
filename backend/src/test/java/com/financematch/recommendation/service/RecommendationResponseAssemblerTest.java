package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.product.type.LoanPurpose;
import com.financematch.product.type.TaxAccountType;
import com.financematch.recommendation.domain.JointRecommendationProduct;
import com.financematch.recommendation.domain.PersonalInvestmentProduct;
import com.financematch.recommendation.domain.PersonalTaxSavingProduct;
import com.financematch.recommendation.domain.RecommendationResult;
import com.financematch.recommendation.dto.RecommendationResponse;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationResponseAssemblerTest {

    private final RecommendationResponseAssembler assembler =
            new RecommendationResponseAssembler();

    @Test
    void assemblesStoredProductsIntoApiResponse() {
        RecommendationResult result = result();
        JointRecommendationProduct deposit =
                jointProduct(
                        11L,
                        RecommendationSlotType.DEPOSIT,
                        101L,
                        true,
                        "KB Star 정기예금");
        deposit.setApplicableBaseRate(new BigDecimal("2.80"));
        deposit.setRecommendationReason("예금 추천 이유");
        JointRecommendationProduct investment =
                jointProduct(
                        12L,
                        RecommendationSlotType.INVESTMENT,
                        201L,
                        true,
                        "RISE 미국S&P500 ETF");
        investment.setRiskLevel(4);
        investment.setAum(3144);
        JointRecommendationProduct loan =
                jointProduct(
                        13L,
                        RecommendationSlotType.LOAN,
                        301L,
                        true,
                        "KB Housing Loan");
        loan.setLoanMaxRate(new BigDecimal("5.48"));
        loan.setLoanPurpose(LoanPurpose.HOUSING);

        RecommendationResponse response =
                assembler.assemble(
                        result,
                        List.of(deposit, investment, loan),
                        List.of(taxSavingProduct()),
                        List.of(investmentProduct()));

        assertEquals(100L, response.recommendationId());
        assertEquals("신혼집 스타터 패키지", response.packageName());
        assertEquals(3, response.packageSlots().size());
        assertEquals(
                "예금 추천 이유",
                response.packageSlots().get(0).products().get(0).recommendationReason());
        assertEquals(
                "연 2.80%",
                response.packageSlots().get(0).products().get(0).comparisonValue());
        assertEquals(
                "위험",
                response.packageSlots().get(1).products().get(0).riskLabel());
        assertEquals(
                3144,
                response.packageSlots().get(1).products().get(0).aum());
        assertEquals(
                LoanPurpose.HOUSING,
                response.packageSlots().get(2).products().get(0).loanPurpose());
        assertEquals(
                TaxAccountType.ISA,
                response.personalTaxSavingRecommendation().products().get(0).accountType());
        assertEquals(
                "절세 추천 이유",
                response.personalTaxSavingRecommendation().products().get(0).recommendationReason());
        assertEquals(
                "중립",
                response.personalInvestmentRecommendation().products().get(0).riskLabel());
        assertEquals(
                28049,
                response.personalInvestmentRecommendation().products().get(0).aum());
        assertEquals(
                "투자 추천 이유",
                response.personalInvestmentRecommendation().products().get(0).recommendationReason());
    }

    @Test
    void returnsNullWhenPersonalRecommendationsAreEmpty() {
        RecommendationResponse response =
                assembler.assemble(result(), List.of(), List.of(), List.of());

        assertEquals(0, response.packageSlots().size());
        assertNull(response.personalTaxSavingRecommendation());
        assertNull(response.personalInvestmentRecommendation());
    }

    @Test
    void rejectsJointSlotWithoutSelectedProduct() {
        JointRecommendationProduct product =
                jointProduct(
                        11L,
                        RecommendationSlotType.DEPOSIT,
                        101L,
                        false,
                        "KB Star 정기예금");
        product.setApplicableBaseRate(new BigDecimal("2.80"));

        assertThrows(
                IllegalStateException.class,
                () -> assembler.assemble(result(), List.of(product), List.of(), List.of()));
    }

    private RecommendationResult result() {
        RecommendationResult result = new RecommendationResult();
        result.setRecommendationId(100L);
        result.setCoupleId(1L);
        result.setFirstGoalType("MARRIAGE");
        result.setHasHighInterestDebt(true);
        return result;
    }

    private JointRecommendationProduct jointProduct(
            Long slotId,
            RecommendationSlotType slotType,
            Long productId,
            boolean selected,
            String productName) {

        JointRecommendationProduct product = new JointRecommendationProduct();
        product.setSlotId(slotId);
        product.setSlotType(slotType);
        product.setProductId(productId);
        product.setRank(1);
        product.setSelected(selected);
        product.setProductName(productName);
        product.setDescription("상품 설명");
        product.setProductUrl("https://www.kbstar.com/product");
        return product;
    }

    private PersonalTaxSavingProduct taxSavingProduct() {
        PersonalTaxSavingProduct product = new PersonalTaxSavingProduct();
        product.setTargetMemberId(1L);
        product.setTargetMemberName("홍길동");
        product.setProductId(301L);
        product.setRank(1);
        product.setProductName("KB증권 중개형 ISA");
        product.setDescription("직접 운용하는 절세 계좌");
        product.setProductUrl("https://www.kbsec.com/isa");
        product.setAccountType(TaxAccountType.ISA);
        product.setRecommendationReason("절세 추천 이유");
        return product;
    }

    private PersonalInvestmentProduct investmentProduct() {
        PersonalInvestmentProduct product = new PersonalInvestmentProduct();
        product.setTargetMemberId(1L);
        product.setTargetMemberName("홍길동");
        product.setProductId(401L);
        product.setRank(1);
        product.setProductName("RISE 미국S&P500 ETF");
        product.setDescription("미국 대표 기업에 분산 투자하는 상품");
        product.setProductUrl("https://www.riseetf.co.kr/product");
        product.setRiskLevel(5);
        product.setAum(28049);
        product.setRecommendationReason("투자 추천 이유");
        return product;
    }
}
