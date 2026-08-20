package com.financematch.report.service;

import com.financematch.report.domain.ReportCoupleDetailSource;
import com.financematch.report.domain.ReportMemberDetailSource;
import com.financematch.report.dto.detail.AiCommentDetail;
import com.financematch.report.dto.detail.AssetDetail;
import com.financematch.report.dto.detail.DebtDetail;
import com.financematch.report.dto.detail.DebtGauge;
import com.financematch.report.dto.detail.GoalDetail;
import com.financematch.report.dto.detail.InvestmentValueDetail;
import com.financematch.report.dto.detail.InvestmentValueRow;
import com.financematch.report.dto.detail.ReportDetails;
import com.financematch.report.dto.detail.TaxDetail;
import com.financematch.report.dto.detail.TaxStatusRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReportDetailFactory {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal COUPLE_MEMBER_COUNT = BigDecimal.valueOf(2);
    private static final BigDecimal WON_PER_EOK = BigDecimal.valueOf(100_000_000L);
    private static final BigDecimal WON_PER_MAN = BigDecimal.valueOf(10_000L);
    private static final BigDecimal ASSET_GRAPH_MAX = BigDecimal.valueOf(200_000_000L);
    private static final BigDecimal DSR_WARNING_THRESHOLD = BigDecimal.valueOf(40);
    private static final BigDecimal DSR_DANGER_THRESHOLD = BigDecimal.valueOf(70);
    private static final BigDecimal DEBT_RATIO_WARNING_THRESHOLD = BigDecimal.valueOf(50);
    private static final BigDecimal DEBT_RATIO_DANGER_THRESHOLD = BigDecimal.valueOf(80);

    public ReportDetails create(
            Long memberId,
            ReportCoupleDetailSource couple,
            ReportMemberDetailSource me,
            ReportMemberDetailSource partner,
            BigDecimal peerAssetMedian) {

        return new ReportDetails(
                createAssetDetail(me, partner, peerAssetMedian),
                createInvestmentValueDetail(me, partner),
                createDebtDetail(me, partner),
                createGoalDetail(couple, me, partner),
                createTaxDetail(me, partner),
                createAiCommentDetail(couple, me, partner, peerAssetMedian));
    }

    private AssetDetail createAssetDetail(
            ReportMemberDetailSource me,
            ReportMemberDetailSource partner,
            BigDecimal peerAssetMedian) {
        BigDecimal referenceAsset = won(peerAssetMedian).multiply(COUPLE_MEMBER_COUNT);
        BigDecimal coupleAsset = won(me.getFinancialAsset()).add(won(partner.getFinancialAsset()));
        BigDecimal ratio = percentage(coupleAsset, referenceAsset, 2);

        String note;
        if (ratio.compareTo(BigDecimal.valueOf(120)) >= 0) {
            note = "또래 커플 대비 높은 수준";
        } else if (ratio.compareTo(BigDecimal.valueOf(100)) >= 0) {
            note = "또래 커플 대비 안정적인 수준";
        } else {
            note = "또래 커플 대비 보완이 필요한 수준";
        }

        return new AssetDetail(
                "또래 커플",
                toEok(referenceAsset),
                assetProgress(referenceAsset),
                "우리 커플",
                toEok(coupleAsset),
                assetProgress(coupleAsset),
                note);
    }

    private InvestmentValueDetail createInvestmentValueDetail(
            ReportMemberDetailSource me, ReportMemberDetailSource partner) {
        return new InvestmentValueDetail(
                List.of(me.getMemberName() + "님", partner.getMemberName() + "님"),
                List.of(
                        new InvestmentValueRow(
                                "투자 경험",
                                gradeLabel(me.getInvestmentExperienceScore()),
                                gradeLabel(partner.getInvestmentExperienceScore()),
                                stepDifference(
                                        me.getInvestmentExperienceScore(),
                                        partner.getInvestmentExperienceScore())),
                        new InvestmentValueRow(
                                "금융상품 이해도",
                                gradeLabel(financialKnowledgeScore(me.getFinancialKnowledge())),
                                gradeLabel(financialKnowledgeScore(partner.getFinancialKnowledge())),
                                stepDifference(
                                        financialKnowledgeScore(me.getFinancialKnowledge()),
                                        financialKnowledgeScore(partner.getFinancialKnowledge()))),
                        new InvestmentValueRow(
                                "손실 감내도",
                                lossToleranceLabel(me.getCapitalPreservationAttitude()),
                                lossToleranceLabel(partner.getCapitalPreservationAttitude()),
                                lossToleranceDifference(
                                        me.getCapitalPreservationAttitude(),
                                        partner.getCapitalPreservationAttitude()))));
    }

    private DebtDetail createDebtDetail(
            ReportMemberDetailSource me, ReportMemberDetailSource partner) {
        BigDecimal totalDebt = won(me.getTotalDebt()).add(won(partner.getTotalDebt()));
        BigDecimal annualDebtPayment =
                won(me.getAnnualDebtPayment()).add(won(partner.getAnnualDebtPayment()));
        BigDecimal annualIncome = won(me.getAnnualIncome()).add(won(partner.getAnnualIncome()));
        BigDecimal financialAsset = won(me.getFinancialAsset()).add(won(partner.getFinancialAsset()));

        BigDecimal dsr = percentage(annualDebtPayment, annualIncome, 1);
        BigDecimal debtRatio = percentage(totalDebt, financialAsset, 0);

        return new DebtDetail(
                "부채 총액: " + WonAmountFormatter.format(toWholeWon(totalDebt)),
                List.of(
                        new DebtGauge(
                                "DSR",
                                dsr,
                                1,
                                "%",
                                DSR_WARNING_THRESHOLD,
                                "40%",
                                clampPercent(dsr),
                                debtStatus(dsr, DSR_WARNING_THRESHOLD, DSR_DANGER_THRESHOLD),
                                "연 소득 대비 연 상환액",
                                "연간 원리금 상환액",
                                WonAmountFormatter.format(toWholeWon(annualDebtPayment))),
                        new DebtGauge(
                                "금융자산 대비 부채",
                                debtRatio,
                                0,
                                "%",
                                null,
                                null,
                                clampPercent(debtRatio),
                                debtStatus(
                                        debtRatio,
                                        DEBT_RATIO_WARNING_THRESHOLD,
                                        DEBT_RATIO_DANGER_THRESHOLD),
                                "총 부채",
                                "부부 연소득",
                                WonAmountFormatter.format(toWholeWon(annualIncome)))));
    }

    private GoalDetail createGoalDetail(
            ReportCoupleDetailSource couple,
            ReportMemberDetailSource me,
            ReportMemberDetailSource partner) {
        BigDecimal targetAmount = won(couple.getTargetAmount());
        BigDecimal expectedAsset = won(couple.getExpectedAsset());
        BigDecimal shortage = targetAmount.subtract(expectedAsset).max(ZERO);
        BigDecimal currentAchievement = percentage(expectedAsset, targetAmount, 0);
        BigDecimal monthlyAvailable =
                won(me.getMonthlyAvailableAmount()).add(won(partner.getMonthlyAvailableAmount()));
        BigDecimal annualIncome = won(me.getAnnualIncome()).add(won(partner.getAnnualIncome()));
        BigDecimal maxMonthlySaving = annualIncome.divide(BigDecimal.valueOf(12), 0, RoundingMode.DOWN);
        BigDecimal additionalMonthlySaving = maxMonthlySaving.subtract(monthlyAvailable).max(ZERO);
        BigDecimal maxScenarioAsset =
                expectedAsset.add(
                        additionalMonthlySaving.multiply(
                                BigDecimal.valueOf(couple.getTargetPeriodMonths() == null
                                        ? 0L
                                        : couple.getTargetPeriodMonths())));
        BigDecimal maxShortage = targetAmount.subtract(maxScenarioAsset).max(ZERO);
        BigDecimal maxAchievement = percentage(maxScenarioAsset, targetAmount, 0);

        return new GoalDetail(
                "목표까지 부족한 금액",
                toMan(shortage),
                WonAmountFormatter.format(toWholeWon(shortage)) + " 부족",
                WonAmountFormatter.format(toWholeWon(expectedAsset)),
                currentAchievement + "%",
                clampPercent(currentAchievement),
                "0만원",
                ZERO,
                toMan(maxMonthlySaving),
                toMan(monthlyAvailable),
                currentAchievement,
                maxAchievement,
                toMan(shortage),
                toMan(maxShortage),
                WonAmountFormatter.format(toWholeWon(targetAmount)));
    }

    private TaxDetail createTaxDetail(
            ReportMemberDetailSource me, ReportMemberDetailSource partner) {
        return new TaxDetail(
                List.of(me.getMemberName() + "님", partner.getMemberName() + "님"),
                List.of(
                        new TaxStatusRow(
                                "ISA",
                                taxStatus(me.isHasIsa(), me.getIsaAnnualDeposit()),
                                taxStatus(partner.isHasIsa(), partner.getIsaAnnualDeposit())),
                        new TaxStatusRow(
                                "IRP",
                                taxStatus(
                                        me.isHasIrp(),
                                        won(me.getIrpAnnualPayment()).add(won(me.getDcAnnualPayment()))),
                                taxStatus(
                                        partner.isHasIrp(),
                                        won(partner.getIrpAnnualPayment())
                                                .add(won(partner.getDcAnnualPayment())))),
                        new TaxStatusRow(
                                "연금저축",
                                taxStatus(me.isHasPensionSaving(), me.getPensionAnnualPayment()),
                                taxStatus(
                                        partner.isHasPensionSaving(),
                                        partner.getPensionAnnualPayment()))));
    }

    private AiCommentDetail createAiCommentDetail(
            ReportCoupleDetailSource couple,
            ReportMemberDetailSource me,
            ReportMemberDetailSource partner,
            BigDecimal peerAssetMedian) {
        if (couple.getExpertComment() != null && !couple.getExpertComment().isBlank()) {
            return new AiCommentDetail("AI 종합 코멘트", "AI가 분석한 종합 평가예요.", couple.getExpertComment());
        }

        return new AiCommentDetail("AI 종합 코멘트", "", "");
    }

    private BigDecimal won(BigDecimal value) {
        return value == null ? ZERO : value.max(ZERO);
    }

    private BigDecimal toWholeWon(BigDecimal value) {
        return won(value).setScale(0, RoundingMode.DOWN);
    }

    private BigDecimal toEok(BigDecimal won) {
        return won(won).divide(WON_PER_EOK, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal toMan(BigDecimal won) {
        return won(won).divide(WON_PER_MAN, 0, RoundingMode.HALF_UP);
    }

    private BigDecimal assetProgress(BigDecimal won) {
        return clampPercent(percentage(won(won), ASSET_GRAPH_MAX, 1));
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator, int scale) {
        BigDecimal safeDenominator = won(denominator);
        if (safeDenominator.compareTo(ZERO) == 0) {
            return ZERO.setScale(scale, RoundingMode.HALF_UP);
        }

        return won(numerator)
                .multiply(HUNDRED)
                .divide(safeDenominator, scale, RoundingMode.HALF_UP);
    }

    private BigDecimal clampPercent(BigDecimal value) {
        if (value.compareTo(ZERO) < 0) {
            return ZERO;
        }
        if (value.compareTo(HUNDRED) > 0) {
            return HUNDRED;
        }
        return value;
    }

    private int financialKnowledgeScore(String value) {
        if ("VERY_LOW".equals(value)) {
            return 1;
        }
        if ("LOW".equals(value)) {
            return 2;
        }
        if ("HIGH".equals(value)) {
            return 4;
        }
        if ("VERY_HIGH".equals(value)) {
            return 5;
        }
        return 3;
    }

    private String gradeLabel(int score) {
        if (score <= 1) {
            return "하";
        }
        if (score == 2) {
            return "중하";
        }
        if (score == 4) {
            return "중상";
        }
        if (score >= 5) {
            return "상";
        }
        return "중";
    }

    private String stepDifference(int meScore, int partnerScore) {
        return Math.abs(meScore - partnerScore) + "단계";
    }

    private int lossTolerancePercent(String value) {
        if ("ZERO".equals(value)) {
            return 0;
        }
        if ("UNDER_10".equals(value)) {
            return 10;
        }
        if ("UNDER_20".equals(value)) {
            return 20;
        }
        if ("UNDER_50".equals(value)) {
            return 50;
        }
        if ("UNDER_70".equals(value)) {
            return 70;
        }
        if ("FULL".equals(value)) {
            return 100;
        }
        return 0;
    }

    private String lossToleranceLabel(String value) {
        return lossTolerancePercent(value) + "%";
    }

    private String lossToleranceDifference(String meValue, String partnerValue) {
        return Math.abs(lossTolerancePercent(meValue) - lossTolerancePercent(partnerValue)) + "%p";
    }

    private String debtStatus(
            BigDecimal value, BigDecimal warningThreshold, BigDecimal dangerThreshold) {
        if (value.compareTo(warningThreshold) < 0) {
            return "안정";
        }
        if (value.compareTo(dangerThreshold) < 0) {
            return "주의";
        }
        return "위험";
    }

    private String taxStatus(boolean hasAccount, BigDecimal annualPayment) {
        if (!hasAccount) {
            return "미개설";
        }
        return won(annualPayment).compareTo(ZERO) > 0 ? "활용" : "미활용";
    }
}
