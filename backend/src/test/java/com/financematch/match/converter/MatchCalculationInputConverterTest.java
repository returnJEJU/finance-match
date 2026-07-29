package com.financematch.match.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MemberCalculationInput;
import com.financematch.match.domain.MatchCoupleData;
import com.financematch.match.domain.MatchMemberData;

class MatchCalculationInputConverterTest {

    private final MatchCalculationInputConverter converter =
            new MatchCalculationInputConverter();

    @Test
    void 조회데이터를_계산입력으로_변환한다() {

        MatchCoupleData couple = new MatchCoupleData();
        couple.setCoupleId(1L);
        couple.setFirstGoalType("HOUSING");
        couple.setTargetAmount(new BigDecimal("350000000"));
        couple.setTargetPeriodMonths(36);

        MatchMemberData memberA = createMember(
                1L,
                "1994-03-15",
                "UNDER_50",
                3,
                "MEDIUM",
                "UNDER_50"
        );

        MatchMemberData memberB = createMember(
                2L,
                "1992-11-20",
                "UNDER_80",
                4,
                "HIGH",
                "UNDER_10"
        );

        MatchCalculationInput result =
                converter.convert(couple, memberA, memberB);

        MemberCalculationInput convertedA = result.getMemberA();
        MemberCalculationInput convertedB = result.getMemberB();

        // 공동 목표 변환 확인
        assertEquals("HOUSING", result.getFirstGoalType());
        assertEquals(
                new BigDecimal("350000000"),
                result.getTargetAmount()
        );
        assertEquals(36, result.getTargetPeriodMonths());

        // 김하나 설문 점수 변환 확인
        assertEquals(3, convertedA.getFinancialAssetRatioScore());
        assertEquals(3, convertedA.getInvestmentExperienceScore());
        assertEquals(3, convertedA.getFinancialKnowledgeScore());
        assertEquals(4, convertedA.getCapitalPreservationScore());

        // 이두리 설문 점수 변환 확인
        assertEquals(4, convertedB.getFinancialAssetRatioScore());
        assertEquals(4, convertedB.getInvestmentExperienceScore());
        assertEquals(4, convertedB.getFinancialKnowledgeScore());
        assertEquals(2, convertedB.getCapitalPreservationScore());

        // 금액 데이터 변환 확인
        assertEquals(
                new BigDecimal("93300000"),
                convertedA.getAgeGroupAssetMedian()
        );
        assertEquals(
                new BigDecimal("90000000"),
                convertedA.getFinancialAsset()
        );
    }

    private MatchMemberData createMember(
            Long memberId,
            String birthDate,
            String financialAssetRatio,
            int investmentExperienceScore,
            String financialKnowledge,
            String capitalPreservationAttitude
    ) {
        MatchMemberData member = new MatchMemberData();

        member.setMemberId(memberId);
        member.setBirthDate(LocalDate.parse(birthDate));

        member.setAgeGroupAssetMedian(new BigDecimal("93300000"));
        member.setFinancialAsset(new BigDecimal("90000000"));

        member.setFinancialAssetRatio(financialAssetRatio);
        member.setInvestmentExperienceScore(investmentExperienceScore);
        member.setFinancialKnowledge(financialKnowledge);
        member.setCapitalPreservationAttitude(
                capitalPreservationAttitude
        );

        return member;
    }
}