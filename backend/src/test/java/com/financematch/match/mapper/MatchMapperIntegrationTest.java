package com.financematch.match.mapper;

import com.financematch.config.RootConfig;
import com.financematch.match.domain.CompatibilityResult;
import com.financematch.match.domain.MatchCoupleData;
import com.financematch.match.domain.MatchMemberData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@ActiveProfiles("local")
@WebAppConfiguration
class MatchMapperIntegrationTest {

    @Autowired
    private MatchMapper matchMapper;

    @Test
    void 로그인_회원의_커플과_공동목표를_조회한다() {
        MatchCoupleData result =
                matchMapper.findCoupleDataByMemberId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getCoupleId());
        assertEquals(1L, result.getInviterId());
        assertEquals(2L, result.getInviteeId());
        assertEquals("HOUSING", result.getFirstGoalType());
        assertEquals(
                new BigDecimal("350000000"),
                result.getTargetAmount()
        );
        assertEquals(36, result.getTargetPeriodMonths());
    }

    @Test
    void 초대받은_회원으로도_같은_커플을_조회한다() {
        MatchCoupleData result =
                matchMapper.findCoupleDataByMemberId(2L);

        assertNotNull(result);
        assertEquals(1L, result.getCoupleId());
        assertEquals(1L, result.getInviterId());
        assertEquals(2L, result.getInviteeId());
    }

    @Test
    void 회원의_궁합_계산_원천데이터를_조회한다() {
        MatchMemberData memberA =
                matchMapper.findMemberDataByMemberId(1L);

        MatchMemberData memberB =
                matchMapper.findMemberDataByMemberId(2L);

        assertNotNull(memberA);
        assertNotNull(memberB);

        assertEquals("김하나", memberA.getMemberName());
        assertEquals(
                new BigDecimal("93300000"),
                memberA.getAgeGroupAssetMedian()
        );
        assertEquals(
                new BigDecimal("90000000"),
                memberA.getFinancialAsset()
        );
        assertEquals(3, memberA.getInvestmentExperienceScore());
        assertEquals("ELIGIBLE", memberA.getTaxEligibilityStatus());

        assertEquals("이두리", memberB.getMemberName());
        assertEquals(
                new BigDecimal("93300000"),
                memberB.getAgeGroupAssetMedian()
        );
        assertEquals(
                new BigDecimal("180000000"),
                memberB.getFinancialAsset()
        );
        assertEquals(4, memberB.getInvestmentExperienceScore());
        assertEquals("ELIGIBLE", memberB.getIsaEligibilityStatus());
    }

    @Test
    @DisplayName("커플 ID로 금융 궁합도 결과를 조회한다")
    void findCompatibilityResultByCoupleId() {

        CompatibilityResult result =
                matchMapper.findCompatibilityResultByCoupleId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getCoupleId());
        assertNotNull(result.getAssetStabilityScore());
        assertNotNull(result.getDebtRepaymentScore());
        assertNotNull(result.getFinancialValueScore());
        assertNotNull(result.getGoalFeasibilityScore());
        assertNotNull(result.getTaxStrategyScore());
        assertTrue(result.isTaxStrategyCalculated());
        assertNotNull(result.getTotalScore());

        System.out.println("조회된 궁합도 총점: " + result.getTotalScore());
    }
}