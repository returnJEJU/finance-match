package com.financematch.personalsurvey.calculator;

import com.financematch.personalsurvey.domain.CoupleInvestmentType;
import com.financematch.personalsurvey.domain.PersonalInvestmentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class CoupleInvestmentTypeCalculatorTest {

    private final CoupleInvestmentTypeCalculator calculator =
            new CoupleInvestmentTypeCalculator();

    // 두 개인 투자성향의 단계 차이 절댓값을 DIFF_0~DIFF_4로 변환하는지 확인
    // @ParameterizedTest --> CSV 한 줄마다 같은 테스트를 반복
    @ParameterizedTest
    @CsvSource({
            "STABLE, STABLE, DIFF_0",
            "STABLE, STABLE_SEEKING, DIFF_1",
            "STABLE, NEUTRAL, DIFF_2",
            "STABLE, AGGRESSIVE, DIFF_3",
            "STABLE, VERY_AGGRESSIVE, DIFF_4",
            "VERY_AGGRESSIVE, STABLE, DIFF_4"
    })
    void calculatesAbsoluteLevelDifference(
            PersonalInvestmentType first,
            PersonalInvestmentType second,
            CoupleInvestmentType expected) {

        CoupleInvestmentType result =
                calculator.calculate(first, second);

        assertEquals(expected, result);
    }

}