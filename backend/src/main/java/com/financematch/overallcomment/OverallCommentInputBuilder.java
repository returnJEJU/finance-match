package com.financematch.overallcomment;

import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MatchCalculationResult;
import com.financematch.match.calculator.MemberCalculationInput;
import com.financematch.overallcomment.dto.AxisFact;
import com.financematch.overallcomment.dto.FirstStepInfo;
import com.financematch.overallcomment.dto.GoalInfo;
import com.financematch.overallcomment.dto.NamesInfo;
import com.financematch.overallcomment.dto.OverallCommentPromptInput;
import com.financematch.report.dto.GoalProgress;
import com.financematch.report.dto.reason.TaxAccountInput;
import com.financematch.report.dto.reason.TaxSavingProfile;
import com.financematch.report.service.GoalProgressFormatter;
import com.financematch.report.service.KoreanNameFormatter;
import com.financematch.report.service.WonAmountFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 종합 코멘트 프롬프트용 재료를 조립한다. {@code MatchService}가 궁합 계산 직후 이미 갖고 있는
 * 값(MatchCalculationInput/Result 등)만 쓰고, 별도 DB 조회는 하지 않는다.
 *
 * <p>목표 부족액·달성률({@link GoalProgressFormatter})과 절세 한도 미달 여부({@link
 * TaxAccountInput#isUnderLimit()})는 {@code report} 패키지의 기존 로직을 그대로 재사용한다(수정
 * 없이 import만 함).
 *
 * <p>강/약 축 선정(팀 미확정, 잠정 규칙): 5축(절세 미해당이면 4축)을 만점 대비 %로 환산해, ratio가
 * {@value #STRONG_RATIO_THRESHOLD} 이상이면 strongAxes, 미만이면 weakAxes로 나눈다 — 개수을 3·2로
 * 고정하지 않고 축 자체 점수로 판정한다(5축이 전부 좋아도 억지로 일부를 약점으로 만들지 않고, 전부
 * 나빠도 억지로 강점을 만들지 않는다). strongAxes는 ratio 내림차순(강점부터), weakAxes는 ratio
 * 오름차순(가장 약한 축부터)이다 — 정렬된 리스트를 그대로 나눠 담은 뒤 weakAxes만 뒤집는다. 퍼센트가
 * 같으면 축을 추가한 순서(자산→부채→가치관→목표→절세)의 역순(약한 쪽 기준이라)이 앞선 쪽이 먼저 온다.
 *
 * <p>금융 자산 축만 예외다(팀 확정): 원점수가 {@value #ASSET_STRONG_SCORE_THRESHOLD}/25 초과면
 * ratio와 무관하게 strongAxes로 판정한다 — 다른 4축은 여전히 {@value #STRONG_RATIO_THRESHOLD}
 * 비율 기준을 그대로 쓴다.
 *
 * <p>축별 facts 생성은 전부 잠정 규칙이다(팀 미확정). 특히 투자 가치관 일치도 축은 지금은 일치도
 * 퍼센트 하나로만 문장을 만든다 — 4문항(diff) 세부 내역은 {@code FinancialValueReasonService}
 * 쪽 로직과 중복 계산을 피하려고 아직 끌어오지 않았다. 더 풍부한 근거가 필요해지면 그 로직을
 * 공용화해서 재사용하면 된다.
 */
@Component
@RequiredArgsConstructor
public class OverallCommentInputBuilder {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final List<String> ACCOUNT_NAMES = List.of("ISA", "IRP", "연금저축");

    // 이 값 이상이면 strongAxes, 미만이면 weakAxes(팀 미확정, 잠정 기준).
    private static final double STRONG_RATIO_THRESHOLD = 0.7;

    // 금융 자산 축 전용 예외 기준(팀 확정): 원점수(25점 만점)가 이 값을 초과하면 ratio와 무관하게
    // strongAxes로 판정한다.
    private static final BigDecimal ASSET_STRONG_SCORE_THRESHOLD = BigDecimal.valueOf(13);

    // common_survey.first_goal_type(GoalType) → 한글 라벨. 프런트(CoupleSurveyPage.vue)의
    // goalOptions와 동일한 문구로 맞춘다 — 백엔드에는 이 라벨 매핑이 따로 없어서 여기서 새로 둔다.
    private static final Map<String, String> GOAL_TYPE_LABELS =
            Map.of(
                    "INVESTMENT", "여유 자금 투자",
                    "RETIREMENT", "노후 자금 마련",
                    "MARRIAGE", "결혼 자금 마련",
                    "HOUSING", "부동산 자금 마련",
                    "SHORT_TERM", "사용예정자금 단기운용");

    private final GoalProgressFormatter goalProgressFormatter;

    public OverallCommentPromptInput build(
            MatchCalculationInput calculationInput,
            MatchCalculationResult calculationResult,
            String meName,
            String partnerName,
            String firstGoalType,
            TaxSavingProfile memberATax,
            TaxSavingProfile memberBTax) {

        NamesInfo names = new NamesInfo(displayName(meName), displayName(partnerName));
        Set<String> allowedNumbers = new LinkedHashSet<>();

        GoalProgress goalProgress =
                goalProgressFormatter.format(
                        calculationResult.getExpectedAsset(), calculationInput.getTargetAmount());
        String monthsLabel = monthsLabel(calculationInput.getTargetPeriodMonths());

        List<RankedAxis> ranked =
                rankAxes(calculationInput, calculationResult, names, monthsLabel, goalProgress, memberATax, memberBTax, allowedNumbers);

        List<AxisFact> strongAxes = new ArrayList<>();
        List<AxisFact> weakAxes = new ArrayList<>();
        for (RankedAxis axis : ranked) {
            AxisFact fact = new AxisFact(axis.name, axis.percent / 100.0, axis.facts);
            if (axis.strong) {
                strongAxes.add(fact);
            } else {
                weakAxes.add(fact);
            }
        }
        // ranked는 percent 내림차순이라 weakAxes도 그대로 담으면 내림차순이 된다 — ratio가
        // 가장 낮은 축이 먼저 오도록(오름차순) 뒤집는다.
        Collections.reverse(weakAxes);

        String shortfall = goalProgress.isAchieved() ? null : goalProgress.getBarLabel();
        GoalInfo goal =
                new GoalInfo(
                        GOAL_TYPE_LABELS.getOrDefault(firstGoalType, "목표 자금 마련"),
                        WonAmountFormatter.format(calculationInput.getTargetAmount()),
                        monthsLabel,
                        goalProgress.getAvailableAsset(),
                        shortfall,
                        goalProgress.getAchievementRate());
        allowedNumbers.add(goal.getTargetAmount());
        allowedNumbers.add(goal.getExpectedAsset());
        allowedNumbers.add(goal.getAchievementRate());
        if (shortfall != null) {
            allowedNumbers.add(shortfall);
        }

        RankedAxis weakest = ranked.get(ranked.size() - 1);
        FirstStepInfo firstStep = new FirstStepInfo(weakest.name, weakest.facts);

        return new OverallCommentPromptInput(
                names, goal, strongAxes, weakAxes, firstStep, new ArrayList<>(allowedNumbers));
    }

    private List<RankedAxis> rankAxes(
            MatchCalculationInput calculationInput,
            MatchCalculationResult calculationResult,
            NamesInfo names,
            String monthsLabel,
            GoalProgress goalProgress,
            TaxSavingProfile memberATax,
            TaxSavingProfile memberBTax,
            Set<String> allowedNumbers) {

        List<RankedAxis> axes = new ArrayList<>();
        axes.add(assetAxis(calculationInput, calculationResult, allowedNumbers));
        axes.add(debtAxis(calculationInput, calculationResult, names, allowedNumbers));
        axes.add(valueAxis(calculationResult, allowedNumbers));
        axes.add(goalAxis(calculationResult, monthsLabel, goalProgress));
        if (calculationResult.isTaxStrategyCalculated()) {
            axes.add(taxAxis(calculationResult, names, memberATax, memberBTax, allowedNumbers));
        }
        // 동점 tie-break: 위에서 추가한 순서(자산→부채→가치관→목표→절세)가 곧 우선순위다.

        axes.sort(Comparator.comparingInt((RankedAxis axis) -> axis.percent).reversed());
        return axes;
    }

    private RankedAxis assetAxis(
            MatchCalculationInput calculationInput, MatchCalculationResult calculationResult, Set<String> allowedNumbers) {
        MemberCalculationInput a = calculationInput.getMemberA();
        MemberCalculationInput b = calculationInput.getMemberB();
        BigDecimal combinedAsset = a.getFinancialAsset().add(b.getFinancialAsset());
        String combinedAssetLabel = WonAmountFormatter.format(combinedAsset);
        int ratioPercent = (int) Math.round(calculationResult.getCoupleAssetRatio() * 100);
        String ratioLabel = ratioPercent + "%";

        allowedNumbers.add(combinedAssetLabel);
        allowedNumbers.add(ratioLabel);

        List<String> facts =
                List.of(
                        "두 분 합산 금융자산 " + combinedAssetLabel,
                        "또래 커플 대비 " + ratioLabel);

        // 예외: 금융 자산 축은 ratio가 아니라 원점수(25점 만점)가 13점을 초과하는지로 강/약을 정한다.
        boolean strong = calculationResult.getAssetStabilityScore().compareTo(ASSET_STRONG_SCORE_THRESHOLD) > 0;
        return new RankedAxis(
                "금융 자산", percentOf(calculationResult.getAssetStabilityScore(), 25), facts, strong);
    }

    private RankedAxis debtAxis(
            MatchCalculationInput calculationInput,
            MatchCalculationResult calculationResult,
            NamesInfo names,
            Set<String> allowedNumbers) {
        List<String> facts = new ArrayList<>();
        addDebtFact(facts, names.getMe(), calculationInput.getMemberA().getTotalDebt(), allowedNumbers);
        addDebtFact(facts, names.getPartner(), calculationInput.getMemberB().getTotalDebt(), allowedNumbers);
        if (facts.isEmpty()) {
            facts.add("두 분 모두 부채가 없어요.");
        }
        int percent = percentOf(calculationResult.getDebtRepaymentScore(), 20);
        return new RankedAxis("부채 상환", percent, facts, isStrongByRatio(percent));
    }

    private void addDebtFact(List<String> facts, String displayName, BigDecimal totalDebt, Set<String> allowedNumbers) {
        if (totalDebt.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        String amount = WonAmountFormatter.format(totalDebt);
        allowedNumbers.add(amount);
        facts.add(displayName + " " + amount);
    }

    private RankedAxis valueAxis(MatchCalculationResult calculationResult, Set<String> allowedNumbers) {
        int percent = percentOf(calculationResult.getFinancialValueScore(), 25);
        String percentLabel = percent + "%";
        allowedNumbers.add(percentLabel);
        return new RankedAxis(
                "투자 가치관",
                percent,
                List.of("두 분의 투자 가치관 일치도 " + percentLabel),
                isStrongByRatio(percent));
    }

    private RankedAxis goalAxis(MatchCalculationResult calculationResult, String monthsLabel, GoalProgress goalProgress) {
        List<String> facts = new ArrayList<>();
        facts.add(monthsLabel + " 뒤 목표의 " + goalProgress.getAchievementRate() + " 도달 예상");
        facts.add(goalProgress.getBarLabel());
        int percent = percentOf(calculationResult.getGoalFeasibilityScore(), 20);
        return new RankedAxis("목표 달성 가능성", percent, facts, isStrongByRatio(percent));
    }

    private RankedAxis taxAxis(
            MatchCalculationResult calculationResult,
            NamesInfo names,
            TaxSavingProfile memberATax,
            TaxSavingProfile memberBTax,
            Set<String> allowedNumbers) {
        List<String> facts = new ArrayList<>();
        facts.addAll(accountFacts(names.getMe(), memberATax, allowedNumbers));
        facts.addAll(accountFacts(names.getPartner(), memberBTax, allowedNumbers));
        if (facts.isEmpty()) {
            facts.add("두 분 모두 " + String.join("·", ACCOUNT_NAMES) + " 한도를 다 채웠어요.");
        }
        int percent = percentOf(calculationResult.getTaxStrategyScore(), 10);
        return new RankedAxis("절세 활용", percent, facts, isStrongByRatio(percent));
    }

    private List<String> accountFacts(String displayName, TaxSavingProfile profile, Set<String> allowedNumbers) {
        List<String> facts = new ArrayList<>();
        addAccountFact(facts, displayName, "ISA", profile.getIsa(), allowedNumbers);
        addAccountFact(facts, displayName, "IRP", profile.getIrp(), allowedNumbers);
        addAccountFact(facts, displayName, "연금저축", profile.getPension(), allowedNumbers);
        return facts;
    }

    private void addAccountFact(
            List<String> facts, String displayName, String accountName, TaxAccountInput account, Set<String> allowedNumbers) {
        if (!account.isOpened()) {
            facts.add(displayName + " " + accountName + " 미개설");
            return;
        }
        if (account.isUnderLimit()) {
            String remaining = WonAmountFormatter.format(account.getAnnualLimit().subtract(account.getContributed()));
            allowedNumbers.add(remaining);
            facts.add(displayName + " " + accountName + " 한도까지 " + remaining + " 남음");
            return;
        }
        String limit = WonAmountFormatter.format(account.getAnnualLimit());
        allowedNumbers.add(limit);
        facts.add(displayName + " " + accountName + " 연 " + limit + " 한도를 채움");
    }

    private int percentOf(BigDecimal score, int maxScore) {
        return score.multiply(HUNDRED).divide(BigDecimal.valueOf(maxScore), 0, RoundingMode.HALF_UP).intValue();
    }

    private boolean isStrongByRatio(int percent) {
        return percent / 100.0 >= STRONG_RATIO_THRESHOLD;
    }

    private String displayName(String fullName) {
        return KoreanNameFormatter.abbreviate(fullName);
    }

    private String monthsLabel(int months) {
        return months % 12 == 0 ? (months / 12) + "년" : months + "개월";
    }

    private static class RankedAxis {
        private final String name;
        private final int percent;
        private final List<String> facts;
        private final boolean strong;

        private RankedAxis(String name, int percent, List<String> facts, boolean strong) {
            this.name = name;
            this.percent = percent;
            this.facts = facts;
            this.strong = strong;
        }
    }
}
