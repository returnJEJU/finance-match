package com.financematch.couple.mapper;

import com.financematch.couple.domain.CoupleProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CoupleMapper {

    // 로그인 회원이 속한 커플의 한줄 소개를 조회한다.
    CoupleProfile findProfileByMemberId(@Param("memberId") Long memberId);

    // 로그인 회원이 속한 커플 row만 한줄 소개를 수정한다.
    int updateProfileMessageByMemberId(
            @Param("memberId") Long memberId,
            @Param("profileMessage") String profileMessage);
}
