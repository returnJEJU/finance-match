package com.financematch.personalsurvey.mapper;

import com.financematch.personalsurvey.domain.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Set;

@Mapper
public interface PersonalSurveyMapper {

    Long lockActiveMemberById(@Param("memberId") Long memberId);

    boolean existsPersonalSurveyByMemberId(@Param("memberId") Long memberId);

    PersonalSurveyCalculationContext findCalculationContext(@Param("memberId") Long memberId);

    int insertPersonalSurvey(PersonalSurvey personalSurvey);

    int insertInvestmentExperiences(
            @Param("personalSurveyId") Long personalSurveyId,
            @Param("experiences") Set<InvestmentExperience> experiences);

    // null 값을 update 하는 것
    int updateMemberInvestmentType(
            @Param("memberId") Long memberId,
            @Param("investmentType") PersonalInvestmentType investmentType);

    ReadyCoupleInvestmentTypes findReadyCoupleInvestmentTypes(@Param("memberId") Long memberId);

    // null 값을 update 하는 것
    int updateCoupleInvestmentType(
            @Param("coupleId") Long coupleId,
            @Param("investmentType") CoupleInvestmentType investmentType);

    PersonalSurveyResult findPersonalSurveyResultByMemberId(
            @Param("memberId") Long memberId);

}
