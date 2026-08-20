package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.recommendation.domain.JointRecommendationProduct;
import com.financematch.recommendation.domain.RecommendationResult;
import com.financematch.recommendation.dto.RecommendationResponse;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationResponseAssemblerCoverageTest {

    private final RecommendationResponseAssembler assembler = new RecommendationResponseAssembler();

    @Test
    void rejectsMissingResult() {
        assertThrows(
                IllegalArgumentException.class,
                () -> assembler.assemble(null, List.of(), List.of(), List.of()));
    }

    @Test
    void createsEverySupportedPackageName() {
        assertEquals("신혼집 스타터 패키지", assembleGoal("MARRIAGE").packageName());
        assertEquals("내 집 마련 스타터 패키지", assembleGoal("HOUSING").packageName());
        assertEquals("노후 준비 스타터 패키지", assembleGoal("RETIREMENT").packageName());
        assertEquals("여유자금 스타터 패키지", assembleGoal("INVESTMENT").packageName());
        assertEquals("목돈 굴리기 스타터 패키지", assembleGoal("SHORT_TERM").packageName());
        assertThrows(IllegalStateException.class, () -> assembleGoal(null));
        assertThrows(IllegalStateException.class, () -> assembleGoal(" "));
        assertThrows(IllegalStateException.class, () -> assembleGoal("UNKNOWN"));
    }

    @Test
    void assemblesSavingsAndEveryRiskLabel() {
        JointRecommendationProduct savings = joint(1L, RecommendationSlotType.SAVINGS, 1L);
        savings.setApplicableBaseRate(new BigDecimal("3.00"));
        assertEquals("적금", assemble(savings).packageSlots().get(0).slotName());

        assertEquals("초고위험", investmentRisk(1));
        assertEquals("초고위험", investmentRisk(2));
        assertEquals("고위험", investmentRisk(3));
        assertEquals("위험", investmentRisk(4));
        assertEquals("중립", investmentRisk(5));
        assertEquals("안정", investmentRisk(6));
    }

    @Test
    void rejectsMissingRatesAndInvalidInvestmentRisks() {
        JointRecommendationProduct deposit = joint(1L, RecommendationSlotType.DEPOSIT, 1L);
        assertThrows(IllegalStateException.class, () -> assemble(deposit));

        JointRecommendationProduct investment = joint(1L, RecommendationSlotType.INVESTMENT, 1L);
        investment.setAum(0);
        assertThrows(IllegalStateException.class, () -> assemble(investment));
        investment.setRiskLevel(0);
        assertThrows(IllegalStateException.class, () -> assemble(investment));
    }

    private RecommendationResponse assembleGoal(String goal) {
        RecommendationResult result = result();
        result.setFirstGoalType(goal);
        return assembler.assemble(result, List.of(), List.of(), List.of());
    }

    private String investmentRisk(int risk) {
        JointRecommendationProduct product = joint(1L, RecommendationSlotType.INVESTMENT, 1L);
        product.setRiskLevel(risk);
        product.setAum(0);
        return assemble(product).packageSlots().get(0).products().get(0).riskLabel();
    }

    private RecommendationResponse assemble(JointRecommendationProduct product) {
        return assembler.assemble(result(), List.of(product), List.of(), List.of());
    }

    private RecommendationResult result() {
        RecommendationResult result = new RecommendationResult();
        result.setRecommendationId(1L);
        result.setFirstGoalType("MARRIAGE");
        return result;
    }

    private JointRecommendationProduct joint(
            Long slotId, RecommendationSlotType type, Long productId) {
        JointRecommendationProduct product = new JointRecommendationProduct();
        product.setSlotId(slotId);
        product.setSlotType(type);
        product.setProductId(productId);
        product.setSelected(true);
        product.setProductName("상품");
        product.setProductUrl("url");
        return product;
    }
}
