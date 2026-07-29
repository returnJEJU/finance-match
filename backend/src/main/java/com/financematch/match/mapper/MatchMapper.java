package com.financematch.match.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.financematch.match.domain.MatchCoupleData;
import com.financematch.match.domain.MatchMemberData;

import com.financematch.match.calculator.MatchCalculationResult;
import com.financematch.match.domain.CompatibilityResult;

@Mapper
public interface MatchMapper {

    MatchCoupleData findCoupleDataByMemberId(
            @Param("memberId") Long memberId
    );

    MatchMemberData findMemberDataByMemberId(
            @Param("memberId") Long memberId
    );

    int insertCompatibilityResult(
            @Param("coupleId") Long coupleId,
            @Param("result") MatchCalculationResult result
    );

    CompatibilityResult findCompatibilityResultByCoupleId(
            @Param("coupleId") Long coupleId
    );
}