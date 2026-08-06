package com.financematch.invitation.mapper;

import com.financematch.invitation.domain.CommonSurvey;
import com.financematch.invitation.domain.CommonSurveyUpdateTarget;
import com.financematch.invitation.dto.CommonSurveyResponse;
import com.financematch.invitation.dto.UpdateCommonSurveyRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvitationMapper {

    String findActiveInviteCodeByMemberId(Long memberId);

    Long lockMemberById(Long memberId);

    boolean existsCoupleByMemberId(Long memberId);

    boolean existsActiveInvitationByMemberId(Long memberId);

    int insertCommonSurvey(CommonSurvey commonSurvey);

    int insertInvitationCode(
            @Param("commonSurveyId") Long commonSurveyId,
            @Param("codeValue") String codeValue);

    CommonSurveyResponse findCommonSurveyByAccessibleMemberId(Long memberId);

    CommonSurveyUpdateTarget findCommonSurveyUpdateTargetForUpdate(Long memberId);

    int updateCommonSurvey(
            @Param("commonSurveyId") Long commonSurveyId,
            @Param("request") UpdateCommonSurveyRequest request);

    int deleteCompatibilityResultByCoupleId(Long coupleId);

    int deleteRecommendationByCoupleId(Long coupleId);
}
