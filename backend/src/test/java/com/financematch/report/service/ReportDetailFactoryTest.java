package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.report.domain.ReportCoupleDetailSource;
import com.financematch.report.domain.ReportMemberDetailSource;
import com.financematch.report.dto.detail.DebtGauge;
import com.financematch.report.dto.detail.ReportDetails;
import com.financematch.report.dto.detail.TaxStatusRow;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReportDetailFactoryTest {

    private final ReportDetailFactory factory = new ReportDetailFactory();

    private ReportMemberDetailSource defaultMember(String name) {
        ReportMemberDetailSource member = new ReportMemberDetailSource();
        ReflectionTestUtils.setField(member, "memberName", name);
        ReflectionTestUtils.setField(member, "annualIncome", BigDecimal.ZERO);
        ReflectionTestUtils.setField(member, "monthlyAvailableAmount", BigDecimal.ZERO);
        ReflectionTestUtils.setField(member, "financialKnowledge", "MEDIUM");
        ReflectionTestUtils.setField(member, "capitalPreservationAttitude", "UNDER_50");
        ReflectionTestUtils.setField(member, "investmentExperienceScore", 3);
        ReflectionTestUtils.setField(member, "financialAsset", BigDecimal.ZERO);
        ReflectionTestUtils.setField(member, "totalDebt", BigDecimal.ZERO);
        ReflectionTestUtils.setField(member, "annualDebtPayment", BigDecimal.ZERO);
        ReflectionTestUtils.setField(member, "hasIsa", false);
        ReflectionTestUtils.setField(member, "hasIrp", false);
        ReflectionTestUtils.setField(member, "hasPensionSaving", false);
        return member;
    }

    private ReportCoupleDetailSource defaultCouple() {
        ReportCoupleDetailSource couple = new ReportCoupleDetailSource();
        ReflectionTestUtils.setField(couple, "targetAmount", BigDecimal.ZERO);
        ReflectionTestUtils.setField(couple, "expectedAsset", BigDecimal.ZERO);
        ReflectionTestUtils.setField(couple, "targetPeriodMonths", null);
        return couple;
    }

    private void set(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }

    // ===== 금융 자산 =====

    @Test
    void 또래_대비_120퍼센트_이상이면_높은_수준으로_평가한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "financialAsset", new BigDecimal("150000000"));

        ReportDetails details =
                factory.create(1L, defaultCouple(), me, partner, new BigDecimal("100000000"));

        assertEquals("또래 커플 대비 높은 수준", details.getAsset().getNote());
    }

    @Test
    void 또래_대비_100에서_120퍼센트면_안정적인_수준으로_평가한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "financialAsset", new BigDecimal("90000000"));

        ReportDetails details =
                factory.create(1L, defaultCouple(), me, partner, new BigDecimal("100000000"));

        assertEquals("또래 커플 대비 안정적인 수준", details.getAsset().getNote());
    }

    @Test
    void 또래_대비_80퍼센트_미만이면_보완이_필요한_수준으로_평가한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "financialAsset", new BigDecimal("10000000"));

        ReportDetails details =
                factory.create(1L, defaultCouple(), me, partner, new BigDecimal("100000000"));

        assertEquals("또래 커플 대비 보완이 필요한 수준", details.getAsset().getNote());
    }

    @Test
    void 또래_중앙값이_없으면_0으로_나누지_않고_보완필요로_처리한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, null);

        assertEquals("또래 커플 대비 보완이 필요한 수준", details.getAsset().getNote());
        assertEquals(BigDecimal.ZERO.setScale(2), details.getAsset().getReferenceValue());
    }

    // ===== 투자 가치관 =====

    @Test
    void 투자_경험_점수_등급_라벨을_전부_확인한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "investmentExperienceScore", 1);
        set(partner, "investmentExperienceScore", 5);

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);

        assertEquals("하", details.getInvestmentValue().getRows().get(0).getMe());
        assertEquals("상", details.getInvestmentValue().getRows().get(0).getPartner());
        assertEquals("4단계", details.getInvestmentValue().getRows().get(0).getMatch());
    }

    @Test
    void 금융지식_점수를_등급별로_전부_확인한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "financialKnowledge", "VERY_LOW");
        set(partner, "financialKnowledge", "VERY_HIGH");

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);

        assertEquals("하", details.getInvestmentValue().getRows().get(1).getMe());
        assertEquals("상", details.getInvestmentValue().getRows().get(1).getPartner());

        set(me, "financialKnowledge", "LOW");
        set(partner, "financialKnowledge", "HIGH");
        details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);
        assertEquals("중하", details.getInvestmentValue().getRows().get(1).getMe());
        assertEquals("중상", details.getInvestmentValue().getRows().get(1).getPartner());
    }

    @Test
    void 손실_감내도를_등급별로_전부_확인한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "capitalPreservationAttitude", "ZERO");
        set(partner, "capitalPreservationAttitude", "FULL");

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);

        assertEquals("0%", details.getInvestmentValue().getRows().get(2).getMe());
        assertEquals("100%", details.getInvestmentValue().getRows().get(2).getPartner());
        assertEquals("100%p", details.getInvestmentValue().getRows().get(2).getMatch());
    }

    // ===== 부채 =====

    @Test
    void DSR이_40미만이면_안정으로_판정한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "annualIncome", new BigDecimal("100000000"));
        set(me, "annualDebtPayment", new BigDecimal("10000000"));

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);

        DebtGauge dsrGauge = details.getDebt().getGauges().get(0);
        assertEquals("안정", dsrGauge.getStatus());
    }

    @Test
    void DSR이_40이상_70미만이면_주의로_판정한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "annualIncome", new BigDecimal("100000000"));
        set(me, "annualDebtPayment", new BigDecimal("50000000"));

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);

        assertEquals("주의", details.getDebt().getGauges().get(0).getStatus());
    }

    @Test
    void DSR이_70이상이면_위험으로_판정한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "annualIncome", new BigDecimal("100000000"));
        set(me, "annualDebtPayment", new BigDecimal("80000000"));

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);

        assertEquals("위험", details.getDebt().getGauges().get(0).getStatus());
    }

    @Test
    void 금융자산_대비_부채_비율도_임계값에_따라_판정한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "financialAsset", new BigDecimal("100000000"));
        set(me, "totalDebt", new BigDecimal("90000000"));

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);

        DebtGauge ratioGauge = details.getDebt().getGauges().get(1);
        assertEquals("위험", ratioGauge.getStatus());
        assertTrue(details.getDebt().getSummary().startsWith("부채 총액"));
    }

    @Test
    void 소득과_자산이_모두_0이어도_0으로_나누지_않는다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);

        assertEquals("안정", details.getDebt().getGauges().get(0).getStatus());
        assertEquals(BigDecimal.ZERO.setScale(1), details.getDebt().getGauges().get(0).getValue());
    }

    // ===== 목표 =====

    @Test
    void 목표_기간이_없으면_추가_저축_시나리오를_0개월로_계산한다() {
        ReportCoupleDetailSource couple = defaultCouple();
        set(couple, "targetAmount", new BigDecimal("100000000"));
        set(couple, "expectedAsset", new BigDecimal("60000000"));
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");

        ReportDetails details = factory.create(1L, couple, me, partner, BigDecimal.ZERO);

        assertEquals("4,000만원 부족", details.getGoal().getShortageBadge());
        assertEquals("60%", details.getGoal().getAchievementRate());
    }

    @Test
    void 목표를_이미_초과했으면_부족액은_0이다() {
        ReportCoupleDetailSource couple = defaultCouple();
        set(couple, "targetAmount", new BigDecimal("100000000"));
        set(couple, "expectedAsset", new BigDecimal("150000000"));
        set(couple, "targetPeriodMonths", 12);
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");

        ReportDetails details = factory.create(1L, couple, me, partner, BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, details.getGoal().getShortageValue());
        assertEquals("0만원 부족", details.getGoal().getShortageBadge());
    }

    // ===== 절세 =====
    //
    //  여기 문자열은 화면에 그대로 출력되는 동시에 프론트의 색 분기 기준이다
    //  (ReportPage.vue 의 taxStatusClass — '활용' 초록 · '미개설' 빨강 · 그 외 보라).
    //  값을 바꾸려면 프론트도 함께 고쳐야 한다. 백엔드만 바꾸면 색이 조용히 어긋난다.

    @Test
    void 계좌_미개설이면_미개설로_표시한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);

        for (TaxStatusRow row : details.getTax().getRows()) {
            assertEquals("미개설", row.getMe());
            assertEquals("미개설", row.getPartner());
        }
    }

    @Test
    void 계좌는_있지만_납입이_없으면_미활용으로_표시한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "hasIsa", true);
        set(me, "isaAnnualDeposit", BigDecimal.ZERO);

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);

        assertEquals("미활용", details.getTax().getRows().get(0).getMe());
    }

    @Test
    void 계좌에_납입중이면_활용으로_표시한다() {
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");
        set(me, "hasIrp", true);
        set(me, "irpAnnualPayment", new BigDecimal("1000000"));
        set(me, "dcAnnualPayment", BigDecimal.ZERO);
        set(me, "hasPensionSaving", true);
        set(me, "pensionAnnualPayment", new BigDecimal("1000000"));

        ReportDetails details = factory.create(1L, defaultCouple(), me, partner, BigDecimal.ZERO);

        assertEquals("활용", details.getTax().getRows().get(1).getMe());
        assertEquals("활용", details.getTax().getRows().get(2).getMe());
    }

    // ===== AI 코멘트 =====

    @Test
    void 코멘트가_없으면_빈_문자열로_내려준다() {
        ReportCoupleDetailSource couple = defaultCouple();
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");

        ReportDetails details = factory.create(1L, couple, me, partner, BigDecimal.ZERO);

        assertEquals("", details.getAiComment().getHeadline());
        assertEquals("", details.getAiComment().getBody());
    }

    @Test
    void 공백만_있는_코멘트도_없는_것으로_취급한다() {
        ReportCoupleDetailSource couple = defaultCouple();
        set(couple, "expertComment", "   ");
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");

        ReportDetails details = factory.create(1L, couple, me, partner, BigDecimal.ZERO);

        assertEquals("", details.getAiComment().getBody());
    }

    @Test
    void 코멘트가_있으면_그대로_담아준다() {
        ReportCoupleDetailSource couple = defaultCouple();
        set(couple, "expertComment", "두 분은 궁합이 좋아요.");
        ReportMemberDetailSource me = defaultMember("철수");
        ReportMemberDetailSource partner = defaultMember("영희");

        ReportDetails details = factory.create(1L, couple, me, partner, BigDecimal.ZERO);

        assertEquals("두 분은 궁합이 좋아요.", details.getAiComment().getBody());
        assertEquals("AI가 분석한 종합 평가예요.", details.getAiComment().getHeadline());
    }
}
