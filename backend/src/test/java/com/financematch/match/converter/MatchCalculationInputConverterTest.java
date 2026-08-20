package com.financematch.match.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MemberCalculationInput;
import com.financematch.match.domain.MatchCoupleData;
import com.financematch.match.domain.MatchMemberData;

class MatchCalculationInputConverterTest {

    private final MatchCalculationInputConverter converter =
            new MatchCalculationInputConverter();

    @Test
    void 조회데이터를_계산입력으로_변환한다() {
        MatchCoupleData couple = createCouple();

        MatchMemberData memberA = createValidMember(1L);
        memberA.setFinancialAssetRatio("UNDER_50");
        memberA.setInvestmentExperienceScore(3);
        memberA.setFinancialKnowledge("MEDIUM");
        memberA.setCapitalPreservationAttitude("UNDER_50");

        MatchMemberData memberB = createValidMember(2L);
        memberB.setFinancialAssetRatio("UNDER_80");
        memberB.setInvestmentExperienceScore(4);
        memberB.setFinancialKnowledge("HIGH");
        memberB.setCapitalPreservationAttitude("UNDER_10");

        MatchCalculationInput result =
                converter.convert(couple, memberA, memberB);

        MemberCalculationInput convertedA = result.getMemberA();
        MemberCalculationInput convertedB = result.getMemberB();

        assertEquals("HOUSING", result.getFirstGoalType());
        assertEquals(
                new BigDecimal("350000000"),
                result.getTargetAmount()
        );
        assertEquals(36, result.getTargetPeriodMonths());

        assertEquals(3, convertedA.getFinancialAssetRatioScore());
        assertEquals(3, convertedA.getInvestmentExperienceScore());
        assertEquals(3, convertedA.getFinancialKnowledgeScore());
        assertEquals(4, convertedA.getCapitalPreservationScore());

        assertEquals(4, convertedB.getFinancialAssetRatioScore());
        assertEquals(4, convertedB.getInvestmentExperienceScore());
        assertEquals(4, convertedB.getFinancialKnowledgeScore());
        assertEquals(2, convertedB.getCapitalPreservationScore());

        assertEquals(
                new BigDecimal("93300000"),
                convertedA.getAgeGroupAssetMedian()
        );
        assertEquals(
                new BigDecimal("90000000"),
                convertedA.getFinancialAsset()
        );
    }

    /*
     * convert() 입력 객체 null 검사
     */

    @Test
    void 커플데이터가_null이면_예외를_던진다() {
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convert(null, memberA, memberB)
        );

        assertEquals(
                "금융 궁합도 계산 데이터가 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 첫번째_회원데이터가_null이면_예외를_던진다() {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberB = createValidMember(2L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convert(couple, null, memberB)
        );

        assertEquals(
                "금융 궁합도 계산 데이터가 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 두번째_회원데이터가_null이면_예외를_던진다() {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convert(couple, memberA, null)
        );

        assertEquals(
                "금융 궁합도 계산 데이터가 필요합니다.",
                exception.getMessage()
        );
    }

    /*
     * 생년월일 null 검사
     */

    @Test
    void 첫번째_회원_생년월일이_null이면_예외를_던진다() {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        memberA.setBirthDate(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convert(couple, memberA, memberB)
        );

        assertEquals(
                "회원 생년월일이 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 두번째_회원_생년월일이_null이면_예외를_던진다() {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        memberB.setBirthDate(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convert(couple, memberA, memberB)
        );

        assertEquals(
                "회원 생년월일이 필요합니다.",
                exception.getMessage()
        );
    }

    /*
     * 금융자산 비중 switch 전체 분기
     */

    @ParameterizedTest
    @CsvSource({
            "UNDER_10, 1",
            "UNDER_30, 2",
            "UNDER_50, 3",
            "UNDER_80, 4",
            "OVER_80, 5"
    })
    void 금융자산비중_코드를_점수로_변환한다(
            String code,
            int expectedScore
    ) {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        memberA.setFinancialAssetRatio(code);

        MatchCalculationInput result =
                converter.convert(couple, memberA, memberB);

        assertEquals(
                expectedScore,
                result.getMemberA().getFinancialAssetRatioScore()
        );
    }

    @Test
    void 금융자산비중이_null이면_예외를_던진다() {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        memberA.setFinancialAssetRatio(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convert(couple, memberA, memberB)
        );

        assertEquals(
                "금융자산 비중 응답이 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 지원하지_않는_금융자산비중이면_예외를_던진다() {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        memberA.setFinancialAssetRatio("INVALID");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convert(couple, memberA, memberB)
        );

        assertEquals(
                "지원하지 않는 금융자산 비중 코드입니다: INVALID",
                exception.getMessage()
        );
    }

    /*
     * 금융 지식 switch 전체 분기
     */

    @ParameterizedTest
    @CsvSource({
            "VERY_LOW, 1",
            "LOW, 2",
            "MEDIUM, 3",
            "HIGH, 4",
            "VERY_HIGH, 5"
    })
    void 금융지식_코드를_점수로_변환한다(
            String code,
            int expectedScore
    ) {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        memberA.setFinancialKnowledge(code);

        MatchCalculationInput result =
                converter.convert(couple, memberA, memberB);

        assertEquals(
                expectedScore,
                result.getMemberA().getFinancialKnowledgeScore()
        );
    }

    @Test
    void 금융지식이_null이면_예외를_던진다() {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        memberA.setFinancialKnowledge(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convert(couple, memberA, memberB)
        );

        assertEquals(
                "금융 지식 응답이 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 지원하지_않는_금융지식이면_예외를_던진다() {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        memberA.setFinancialKnowledge("INVALID");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convert(couple, memberA, memberB)
        );

        assertEquals(
                "지원하지 않는 금융 지식 코드입니다: INVALID",
                exception.getMessage()
        );
    }

    /*
     * 원금 보존 switch 전체 분기
     */

    @ParameterizedTest
    @CsvSource({
            "ZERO, 1",
            "UNDER_10, 2",
            "UNDER_20, 3",
            "UNDER_50, 4",
            "UNDER_70, 5",
            "FULL, 6"
    })
    void 원금보존_코드를_점수로_변환한다(
            String code,
            int expectedScore
    ) {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        memberA.setCapitalPreservationAttitude(code);

        MatchCalculationInput result =
                converter.convert(couple, memberA, memberB);

        assertEquals(
                expectedScore,
                result.getMemberA().getCapitalPreservationScore()
        );
    }

    @Test
    void 원금보존응답이_null이면_예외를_던진다() {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        memberA.setCapitalPreservationAttitude(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convert(couple, memberA, memberB)
        );

        assertEquals(
                "원금 보존 응답이 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    void 지원하지_않는_원금보존응답이면_예외를_던진다() {
        MatchCoupleData couple = createCouple();
        MatchMemberData memberA = createValidMember(1L);
        MatchMemberData memberB = createValidMember(2L);

        memberA.setCapitalPreservationAttitude("INVALID");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convert(couple, memberA, memberB)
        );

        assertEquals(
                "지원하지 않는 원금 보존 코드입니다: INVALID",
                exception.getMessage()
        );
    }

    /*
     * 테스트 데이터 생성
     */

    private MatchCoupleData createCouple() {
        MatchCoupleData couple = new MatchCoupleData();
        couple.setCoupleId(1L);
        couple.setFirstGoalType("HOUSING");
        couple.setTargetAmount(new BigDecimal("350000000"));
        couple.setTargetPeriodMonths(36);
        return couple;
    }

    private MatchMemberData createValidMember(Long memberId) {
        MatchMemberData member = new MatchMemberData();

        member.setMemberId(memberId);
        member.setBirthDate(LocalDate.of(1994, 3, 15));

        member.setAgeGroupAssetMedian(
                new BigDecimal("93300000")
        );
        member.setFinancialAsset(
                new BigDecimal("90000000")
        );

        member.setAnnualIncome(
                new BigDecimal("50000000")
        );
        member.setTotalDebt(
                new BigDecimal("10000000")
        );
        member.setAnnualDebtPayment(
                new BigDecimal("2000000")
        );

        member.setFinancialAssetRatio("UNDER_50");
        member.setInvestmentExperienceScore(3);
        member.setFinancialKnowledge("MEDIUM");
        member.setCapitalPreservationAttitude("UNDER_50");

        member.setMonthlyAvailableAmount(
                new BigDecimal("1000000")
        );
        member.setPensionSavingBalance(
                new BigDecimal("5000000")
        );
        member.setIrpBalance(BigDecimal.ZERO);
        member.setPensionAnnualPayment(
                new BigDecimal("6000000")
        );
        member.setIrpAnnualPayment(BigDecimal.ZERO);

        member.setDcAnnualPayment(BigDecimal.ZERO);
        member.setIsaAnnualDeposit(
                new BigDecimal("10000000")
        );
        member.setTaxEligibilityStatus("ELIGIBLE");
        member.setIsaEligibilityStatus("ELIGIBLE");

        member.setHasIsa(true);
        member.setHasIrp(false);
        member.setHasPensionSaving(true);

        return member;
    }
}