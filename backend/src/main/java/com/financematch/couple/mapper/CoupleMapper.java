package com.financematch.couple.mapper;

import com.financematch.couple.domain.CoupleProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.financematch.couple.domain.InvitationTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CoupleMapper {

    // 로그인 회원이 속한 커플의 한줄 소개를 조회한다.
    CoupleProfile findProfileByMemberId(@Param("memberId") Long memberId);

    // 로그인 회원이 속한 커플 row만 한줄 소개를 수정한다.
    int updateProfileMessageByMemberId(
            @Param("memberId") Long memberId,
            @Param("profileMessage") String profileMessage);

    // 연결된 파트너 이름을 응답하기 위한 메서드
    String findMemberNameById(@Param("memberId") Long memberId);

    // 초대코드 조회 및 잠금
    InvitationTarget findInvitationTargetForUpdate(
            @Param("inviteCode") String inviteCode);

    // 회원 A, B 잠금
    List<Long> lockMembersForUpdate(
            @Param("firstMemberId") Long firstMemberId,
            @Param("secondMemberId") Long secondMemberId);

    // 커플 연동 여부
    boolean existsCoupleByMemberId(@Param("memberId") Long memberId);

    // A(초대코드 입력자)의 기생성 정보 삭제 (초대코드, 공동설문, 개인설문, 투자성향)
    int deleteInvitationByMemberId(@Param("memberId") Long memberId);
    int deleteCommonSurveyByMemberId(@Param("memberId") Long memberId);
    int deleteInvestmentExperiencesByMemberId(@Param("memberId") Long memberId);
    int deletePersonalSurveyByMemberId(@Param("memberId") Long memberId);
    int clearInvestmentType(@Param("memberId") Long memberId);

    // 커플 생성
    int insertCouple(
            @Param("inviterId") Long inviterId,
            @Param("inviteeId") Long inviteeId,
            @Param("invitationCodeId") Long invitationCodeId);

    // 초대코드 사용 표시
    int markInvitationUsed(
            @Param("invitationCodeId") Long invitationCodeId);

}
