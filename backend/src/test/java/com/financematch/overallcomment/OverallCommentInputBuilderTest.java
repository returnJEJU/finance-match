package com.financematch.overallcomment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MatchCalculationResult;
import com.financematch.match.calculator.MemberCalculationInput;
import com.financematch.overallcomment.dto.AxisFact;
import com.financematch.overallcomment.dto.OverallCommentPromptInput;
import com.financematch.report.dto.reason.TaxAccountInput;
import com.financematch.report.dto.reason.TaxSavingProfile;
import com.financematch.report.service.GoalProgressFormatter;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class OverallCommentInputBuilderTest {

    private final OverallCommentInputBuilder builder =
            new OverallCommentInputBuilder(new GoalProgressFormatter());

    private static final TaxAccountInput OPEN_FULL =
            new TaxAccountInput(true, new BigDecimal(20_000_000), new BigDecimal(20_000_000));
    private static final TaxSavingProfile NO_TAX_ROOM =
            new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);

    private MemberCalculationInput member(BigDecimal financialAsset, BigDecimal totalDebt) {
        return MemberCalculationInput.builder().financialAsset(financialAsset).totalDebt(totalDebt).build();
    }

    private MatchCalculationInput.MatchCalculationInputBuilder baseInput() {
        return MatchCalculationInput.builder()
                .memberA(member(new BigDecimal("50000000"), BigDecimal.ZERO))
                .memberB(member(new BigDecimal("30000000"), BigDecimal.ZERO))
                .targetAmount(new BigDecimal("100000000"))
                .targetPeriodMonths(60);
    }

    private MatchCalculationResult.MatchCalculationResultBuilder baseResult() {
        return MatchCalculationResult.builder()
                .coupleAssetRatio(1.1)
                .expectedAsset(new BigDecimal("120000000"))
                .taxStrategyCalculated(true);
    }

    @Test
    void ratio가_0_7_이상이면_strongAxes_미만이면_weakAxes로_나뉜다() {
        MatchCalculationInput input = baseInput().build();
        MatchCalculationResult result =
                baseResult()
                        .assetStabilityScore(new BigDecimal("17.75")) // 71% → strong
                        .debtRepaymentScore(new BigDecimal("13.00")) // 65% → weak
                        .financialValueScore(new BigDecimal("17.50")) // 70% (경계값) → strong
                        .goalFeasibilityScore(new BigDecimal("13.60")) // 68% → weak
                        .taxStrategyScore(new BigDecimal("7.50")) // 75% → strong
                        .build();

        OverallCommentPromptInput promptInput =
                builder.build(input, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);

        // 개수를 3/2로 고정하지 않고, 각 축이 자기 ratio로만 판정된다 — ratio 내림차순 순서도 함께 확인.
        assertEquals(
                List.of("절세 활용", "금융 자산", "투자 가치관"), namesOf(promptInput.getStrongAxes()));
        assertEquals(List.of("부채 상환", "목표 달성 가능성"), namesOf(promptInput.getWeakAxes()));
        // 경계값(70%)은 "이상"이라 strongAxes에 포함된다.
        assertEquals(0.7, promptInput.getStrongAxes().get(2).getRatio());
        // 가장 약한 축(부채, 65%)이 firstStep 대상이 된다.
        assertEquals("부채 상환", promptInput.getFirstStep().getReason());
    }

    @Test
    void 금융_자산은_ratio_대신_원점수가_13_초과인지로_강약을_정한다() {
        MatchCalculationInput input = baseInput().build();
        MatchCalculationResult result =
                baseResult()
                        .assetStabilityScore(new BigDecimal("14.00")) // 56% → ratio 기준이면 weak, 예외(13 초과)면 strong
                        .debtRepaymentScore(new BigDecimal("6.00")) // 30% → weak
                        .financialValueScore(new BigDecimal("7.50")) // 30% → weak
                        .goalFeasibilityScore(new BigDecimal("6.00")) // 30% → weak
                        .taxStrategyScore(new BigDecimal("3.00")) // 30% → weak
                        .build();

        OverallCommentPromptInput promptInput =
                builder.build(input, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);

        assertEquals(List.of("금융 자산"), namesOf(promptInput.getStrongAxes()));
        assertEquals(0.56, promptInput.getStrongAxes().get(0).getRatio());
    }

    @Test
    void 금융_자산_원점수가_정확히_13이면_초과가_아니라_weakAxes로_간다() {
        MatchCalculationInput input = baseInput().build();
        MatchCalculationResult result =
                baseResult()
                        .assetStabilityScore(new BigDecimal("13.00")) // 52%, 13 "초과"가 아니므로 weak
                        .debtRepaymentScore(new BigDecimal("16.00")) // 80% → strong(일반 축은 ratio 그대로)
                        .financialValueScore(new BigDecimal("7.50"))
                        .goalFeasibilityScore(new BigDecimal("6.00"))
                        .taxStrategyScore(new BigDecimal("3.00"))
                        .build();

        OverallCommentPromptInput promptInput =
                builder.build(input, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);

        assertTrue(namesOf(promptInput.getWeakAxes()).contains("금융 자산"));
        assertTrue(namesOf(promptInput.getStrongAxes()).contains("부채 상환"));
    }

    @Test
    void 오축_전부_강점이면_weakAxes는_비고_firstStep은_동점_tie_break_마지막_축이다() {
        MatchCalculationInput input = baseInput().build();
        MatchCalculationResult result =
                baseResult()
                        .assetStabilityScore(new BigDecimal("20.00")) // 80%
                        .debtRepaymentScore(new BigDecimal("16.00")) // 80%
                        .financialValueScore(new BigDecimal("20.00")) // 80%
                        .goalFeasibilityScore(new BigDecimal("16.00")) // 80%
                        .taxStrategyScore(new BigDecimal("8.00")) // 80%
                        .build();

        OverallCommentPromptInput promptInput =
                builder.build(input, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);

        assertEquals(5, promptInput.getStrongAxes().size());
        assertTrue(promptInput.getWeakAxes().isEmpty());
        // 전부 동점(80%)이면 축을 추가한 순서(자산→부채→가치관→목표→절세) 중 마지막인 절세가 firstStep.
        assertEquals("절세 활용", promptInput.getFirstStep().getReason());
    }

    @Test
    void 오축_전부_약점이면_strongAxes는_비고_전부_weakAxes로_간다() {
        MatchCalculationInput input = baseInput().build();
        MatchCalculationResult result =
                baseResult()
                        .assetStabilityScore(new BigDecimal("7.50")) // 30%
                        .debtRepaymentScore(new BigDecimal("6.00")) // 30%
                        .financialValueScore(new BigDecimal("7.50")) // 30%
                        .goalFeasibilityScore(new BigDecimal("6.00")) // 30%
                        .taxStrategyScore(new BigDecimal("3.00")) // 30%
                        .build();

        OverallCommentPromptInput promptInput =
                builder.build(input, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);

        assertTrue(promptInput.getStrongAxes().isEmpty());
        assertEquals(5, promptInput.getWeakAxes().size());
    }

    @Test
    void 절세_축이_해당_없으면_4축만_대상으로_판정한다() {
        MatchCalculationInput input = baseInput().build();
        MatchCalculationResult result =
                baseResult()
                        .assetStabilityScore(new BigDecimal("25.00")) // 100% → strong
                        .debtRepaymentScore(new BigDecimal("12.00")) // 60% → weak
                        .financialValueScore(new BigDecimal("20.00")) // 80% → strong
                        .goalFeasibilityScore(new BigDecimal("8.00")) // 40% → weak
                        .taxStrategyScore(new BigDecimal("10.00")) // 해당 없으므로 무시돼야 함
                        .taxStrategyCalculated(false)
                        .build();

        OverallCommentPromptInput promptInput =
                builder.build(input, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);

        assertEquals(2, promptInput.getStrongAxes().size());
        assertEquals(List.of("목표 달성 가능성", "부채 상환"), namesOf(promptInput.getWeakAxes()));
        assertEquals("목표 달성 가능성", promptInput.getFirstStep().getReason());
        // 절세 축 관련 facts(계좌 문구)가 전혀 섞여 들어가면 안 된다.
        assertTrue(
                allAxes(promptInput).stream()
                        .flatMap(axis -> axis.getFacts().stream())
                        .noneMatch(fact -> fact.contains("ISA") || fact.contains("IRP")));
    }

    @Test
    void 부채가_있는_사람만_언급되고_없으면_없다는_문구가_들어간다() {
        MatchCalculationInput bothZero = baseInput().build();
        MatchCalculationResult result = baseResult().assetStabilityScore(new BigDecimal("20.00"))
                .debtRepaymentScore(new BigDecimal("20.00"))
                .financialValueScore(new BigDecimal("20.00"))
                .goalFeasibilityScore(new BigDecimal("15.00"))
                .taxStrategyScore(new BigDecimal("8.00"))
                .build();

        OverallCommentPromptInput zeroDebtInput =
                builder.build(bothZero, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);
        AxisFact debtAxisZero = axisNamed(zeroDebtInput, "부채 상환");
        assertEquals(List.of("두 분 모두 부채가 없어요."), debtAxisZero.getFacts());

        MatchCalculationInput oneHasDebt =
                baseInput()
                        .memberA(member(new BigDecimal("50000000"), new BigDecimal("10000000")))
                        .memberB(member(new BigDecimal("30000000"), BigDecimal.ZERO))
                        .build();
        OverallCommentPromptInput debtInput =
                builder.build(oneHasDebt, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);
        AxisFact debtAxis = axisNamed(debtInput, "부채 상환");
        assertEquals(List.of("철수 1,000만원"), debtAxis.getFacts());
        assertTrue(debtInput.getAllowedNumbers().contains("1,000만원"));
    }

    @Test
    void 절세_계좌_상태별로_미개설_한도미달_한도채움_문구가_갈린다() {
        TaxAccountInput underLimitIsa =
                new TaxAccountInput(true, new BigDecimal(10_000_000), new BigDecimal(20_000_000));
        TaxSavingProfile memberA =
                new TaxSavingProfile(underLimitIsa, OPEN_FULL, TaxAccountInput.unopened());

        MatchCalculationInput input = baseInput().build();
        MatchCalculationResult result =
                baseResult()
                        .assetStabilityScore(new BigDecimal("20.00"))
                        .debtRepaymentScore(new BigDecimal("15.00"))
                        .financialValueScore(new BigDecimal("20.00"))
                        .goalFeasibilityScore(new BigDecimal("15.00"))
                        .taxStrategyScore(new BigDecimal("5.00"))
                        .build();

        OverallCommentPromptInput promptInput =
                builder.build(input, result, "김철수", "이영희", "HOUSING", memberA, NO_TAX_ROOM);

        AxisFact taxAxis = axisNamed(promptInput, "절세 활용");
        assertTrue(taxAxis.getFacts().contains("철수 연금저축 미개설"));
        assertTrue(taxAxis.getFacts().contains("철수 ISA 한도까지 1,000만원 남음"));
        assertTrue(taxAxis.getFacts().contains("철수 IRP 연 2,000만원 한도를 채움"));
        assertTrue(promptInput.getAllowedNumbers().contains("1,000만원"));
        assertTrue(promptInput.getAllowedNumbers().contains("2,000만원"));
    }

    @Test
    void goal_필드는_GoalProgressFormatter_결과와_purpose를_그대로_담는다() {
        MatchCalculationInput input = baseInput().build(); // targetAmount 1억, targetPeriodMonths 60(=5년)
        MatchCalculationResult result =
                baseResult()
                        .assetStabilityScore(new BigDecimal("20.00"))
                        .debtRepaymentScore(new BigDecimal("15.00"))
                        .financialValueScore(new BigDecimal("20.00"))
                        .goalFeasibilityScore(new BigDecimal("15.00"))
                        .taxStrategyScore(new BigDecimal("5.00"))
                        .expectedAsset(new BigDecimal("120000000")) // 1억 목표 대비 2000만 초과
                        .build();

        OverallCommentPromptInput promptInput =
                builder.build(input, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);

        assertEquals("부동산 자금 마련", promptInput.getGoal().getPurpose());
        assertEquals("1억원", promptInput.getGoal().getTargetAmount());
        assertEquals("5년", promptInput.getGoal().getMonthsLabel());
        assertEquals("1억 2,000만원", promptInput.getGoal().getExpectedAsset());
        assertEquals("120%", promptInput.getGoal().getAchievementRate());
        assertNull(promptInput.getGoal().getShortfall()); // 초과 달성이라 부족액 없음
    }

    @Test
    void 목표를_아직_달성하지_못했으면_shortfall이_채워진다() {
        MatchCalculationInput input = baseInput().build(); // targetAmount 1억
        MatchCalculationResult result =
                baseResult()
                        .assetStabilityScore(new BigDecimal("20.00"))
                        .debtRepaymentScore(new BigDecimal("15.00"))
                        .financialValueScore(new BigDecimal("20.00"))
                        .goalFeasibilityScore(new BigDecimal("15.00"))
                        .taxStrategyScore(new BigDecimal("5.00"))
                        .expectedAsset(new BigDecimal("80000000")) // 1억 목표에 못 미침
                        .build();

        OverallCommentPromptInput promptInput =
                builder.build(input, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);

        assertTrue(promptInput.getGoal().getShortfall() != null);
        assertTrue(promptInput.getAllowedNumbers().contains(promptInput.getGoal().getShortfall()));
    }

    @Test
    void 목표_기간이_12의_배수가_아니면_개월_단위로_표기한다() {
        MatchCalculationInput input = baseInput().targetPeriodMonths(58).build();
        MatchCalculationResult result =
                baseResult()
                        .assetStabilityScore(new BigDecimal("20.00"))
                        .debtRepaymentScore(new BigDecimal("15.00"))
                        .financialValueScore(new BigDecimal("20.00"))
                        .goalFeasibilityScore(new BigDecimal("15.00"))
                        .taxStrategyScore(new BigDecimal("5.00"))
                        .build();

        OverallCommentPromptInput promptInput =
                builder.build(input, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);

        assertEquals("58개월", promptInput.getGoal().getMonthsLabel());
    }

    @Test
    void 이름은_님을_붙이지_않은_축약형_그대로_들어간다() {
        MatchCalculationInput input = baseInput().build();
        MatchCalculationResult result =
                baseResult()
                        .assetStabilityScore(new BigDecimal("20.00"))
                        .debtRepaymentScore(new BigDecimal("15.00"))
                        .financialValueScore(new BigDecimal("20.00"))
                        .goalFeasibilityScore(new BigDecimal("15.00"))
                        .taxStrategyScore(new BigDecimal("5.00"))
                        .build();

        OverallCommentPromptInput promptInput =
                builder.build(input, result, "김철수", "이영희", "HOUSING", NO_TAX_ROOM, NO_TAX_ROOM);

        assertEquals("철수", promptInput.getNames().getMe());
        assertEquals("영희", promptInput.getNames().getPartner());
    }

    private List<String> namesOf(List<AxisFact> axes) {
        return axes.stream().map(AxisFact::getName).collect(Collectors.toList());
    }

    private List<AxisFact> allAxes(OverallCommentPromptInput input) {
        return java.util.stream.Stream.concat(input.getStrongAxes().stream(), input.getWeakAxes().stream())
                .collect(Collectors.toList());
    }

    private AxisFact axisNamed(OverallCommentPromptInput input, String name) {
        return allAxes(input).stream()
                .filter(axis -> axis.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("축을 찾지 못함: " + name));
    }
}
