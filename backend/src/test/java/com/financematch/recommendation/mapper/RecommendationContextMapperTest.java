package com.financematch.recommendation.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.financematch.config.RootConfig;
import com.financematch.recommendation.domain.RecommendationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@WebAppConfiguration
class RecommendationContextMapperTest {

    @Autowired
    private RecommendationContextMapper recommendationContextMapper;

    @Test
    void 회원ID로_부부_추천정보를_조회한다() {

        // given
        Long memberId = 1L; // 실제 DB에 존재하는 ACTIVE 회원 ID로 변경

        // when
        RecommendationContext context =
                recommendationContextMapper.findByMemberId(memberId);

        // then
        assertNotNull(context);

        assertNotNull(context.getCoupleId());
        assertNotNull(context.getInviterId());
        assertNotNull(context.getInviteeId());

        System.out.println("coupleId = " + context.getCoupleId());
        System.out.println("inviterId = " + context.getInviterId());
        System.out.println("inviteeId = " + context.getInviteeId());

        System.out.println(
                "inviterInvestmentType = "
                        + context.getInviterInvestmentType());

        System.out.println(
                "inviteeInvestmentType = "
                        + context.getInviteeInvestmentType());

        System.out.println(
                "targetPeriodMonths = "
                        + context.getTargetPeriodMonths());

        System.out.println(
                "availableBalance = "
                        + context.getAvailableBalance());

        System.out.println(
                "hasHighRateDebt = "
                        + context.isHasHighRateDebt());
    }
}