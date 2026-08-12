package com.financematch.couple.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.config.RootConfig;
import com.financematch.couple.domain.CoupleDisconnectTarget;
import com.financematch.couple.domain.CoupleProfile;
import com.financematch.couple.domain.InvitationTarget;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@ActiveProfiles("local")
@WebAppConfiguration
@Transactional
class CoupleMapperTest {

    @Autowired
    private CoupleMapper coupleMapper;

    @Autowired
    private DataSource dataSource;

    /** 커플 양쪽 회원에게 본인 기준 프로필을 반환하고 관계없는 회원의 조회·수정을 차단하는지 검증한다. */
    @Test
    void readsAndUpdatesProfileOnlyForCoupleMembers() {
        Long inviterId = insertMember("profile-inviter");
        Long inviteeId = insertMember("profile-invitee");
        Long unrelatedMemberId = insertMember("profile-unrelated");
        Long commonSurveyId = insertCommonSurvey(inviterId);
        Long invitationCodeId = insertInvitationCode(commonSurveyId);
        Long coupleId = insertCouple(inviterId, inviteeId, invitationCodeId);

        CoupleProfile inviterProfile = coupleMapper.findProfileByMemberId(inviterId);
        CoupleProfile inviteeProfile = coupleMapper.findProfileByMemberId(inviteeId);

        assertProfile(inviterProfile, coupleId, "profile-inviter", "profile-invitee", null);
        assertProfile(inviteeProfile, coupleId, "profile-invitee", "profile-inviter", null);
        assertNull(coupleMapper.findProfileByMemberId(unrelatedMemberId));

        assertEquals(
                0,
                coupleMapper.updateProfileMessageByMemberId(
                        unrelatedMemberId,
                        "수정할 수 없는 소개"));
        assertEquals(
                1,
                coupleMapper.updateProfileMessageByMemberId(
                        inviterId,
                        "함께 만드는 금융 계획"));
        assertEquals(
                "함께 만드는 금융 계획",
                coupleMapper.findProfileByMemberId(inviteeId).getProfileMessage());
    }

    /** 초대코드로 커플 생성에 필요한 대상 정보가 정확히 매핑되고 유효하지 않은 코드는 조회되지 않는지 검증한다. */
    @Test
    void findsInvitationTargetAndActiveMemberName() {
        Long inviterId = insertMember("target-inviter");
        Long commonSurveyId = insertCommonSurvey(inviterId);
        String codeValue = invitationCode();
        Long invitationCodeId = insertInvitationCode(commonSurveyId, codeValue);

        InvitationTarget target = coupleMapper.findInvitationTargetForUpdate(codeValue);

        assertNotNull(target);
        assertEquals(invitationCodeId, target.getInvitationCodeId());
        assertEquals(commonSurveyId, target.getCommonSurveyId());
        assertEquals(inviterId, target.getInviterId());
        assertEquals("ACTIVE", target.getStatus());
        assertEquals("target-inviter", coupleMapper.findMemberNameById(inviterId));
        assertNull(coupleMapper.findInvitationTargetForUpdate("UNKNOWN-CODE"));

        Long withdrawnMemberId = insertMember("target-withdrawn");
        jdbcTemplate().update(
                "UPDATE member SET status = 'WITHDRAWN', deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                withdrawnMemberId);
        assertNull(coupleMapper.findMemberNameById(withdrawnMemberId));
    }

    /** 회원 잠금 조회는 활성 회원만 ID 순서대로 반환하고 탈퇴 회원은 제외하는지 검증한다. */
    @Test
    void locksOnlyActiveMembersInIdOrder() {
        Long firstMemberId = insertMember("lock-first");
        Long secondMemberId = insertMember("lock-second");
        Long withdrawnMemberId = insertMember("lock-withdrawn");
        jdbcTemplate().update(
                "UPDATE member SET status = 'WITHDRAWN', deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                withdrawnMemberId);

        List<Long> activeMemberIds =
                coupleMapper.lockMembersForUpdate(secondMemberId, firstMemberId);
        List<Long> includingWithdrawn =
                coupleMapper.lockMembersForUpdate(firstMemberId, withdrawnMemberId);

        assertEquals(List.of(firstMemberId, secondMemberId), activeMemberIds);
        assertEquals(List.of(firstMemberId), includingWithdrawn);
    }

    /** 커플을 생성하면 양쪽 회원의 연결 상태가 조회되고 초대코드는 한 번만 USED로 변경되는지 검증한다. */
    @Test
    void insertsCoupleAndMarksInvitationUsed() {
        Long inviterId = insertMember("create-inviter");
        Long inviteeId = insertMember("create-invitee");
        Long unrelatedMemberId = insertMember("create-unrelated");
        Long commonSurveyId = insertCommonSurvey(inviterId);
        Long invitationCodeId = insertInvitationCode(commonSurveyId);

        assertFalse(coupleMapper.existsCoupleByMemberId(inviterId));
        assertEquals(1, coupleMapper.insertCouple(inviterId, inviteeId, invitationCodeId));
        assertTrue(coupleMapper.existsCoupleByMemberId(inviterId));
        assertTrue(coupleMapper.existsCoupleByMemberId(inviteeId));
        assertFalse(coupleMapper.existsCoupleByMemberId(unrelatedMemberId));

        assertEquals(1, coupleMapper.markInvitationUsed(invitationCodeId));
        assertEquals(0, coupleMapper.markInvitationUsed(invitationCodeId));
        assertEquals(
                "USED",
                jdbcTemplate().queryForObject(
                        "SELECT status FROM invitation_code WHERE id = ?",
                        String.class,
                        invitationCodeId));
    }

    /** 커플 연결 전에 초대받는 회원이 만든 초대·설문·투자 정보를 모두 정리할 수 있는지 검증한다. */
    @Test
    void deletesPreviousOnboardingDataBeforeCoupleConnection() {
        Long memberId = insertMember("cleanup-member");
        Long commonSurveyId = insertCommonSurvey(memberId);
        insertInvitationCode(commonSurveyId);
        Long personalSurveyId = insertPersonalSurvey(memberId);
        insertInvestmentExperiences(personalSurveyId);
        jdbcTemplate().update(
                "UPDATE member SET investment_type = 'NEUTRAL' WHERE id = ?",
                memberId);

        assertEquals(1, coupleMapper.deleteInvitationByMemberId(memberId));
        assertEquals(1, coupleMapper.deleteCommonSurveyByMemberId(memberId));
        assertEquals(2, coupleMapper.deleteInvestmentExperiencesByMemberId(memberId));
        assertEquals(1, coupleMapper.deletePersonalSurveyByMemberId(memberId));
        assertEquals(1, coupleMapper.clearInvestmentType(memberId));

        assertEquals(0, countByMember("common_survey", memberId));
        assertEquals(0, countByMember("personal_survey", memberId));
        assertEquals(
                0,
                countByPersonalSurveyId(
                        "personal_survey_investment_experience",
                        personalSurveyId));
        assertNull(
                jdbcTemplate().queryForObject(
                        "SELECT investment_type FROM member WHERE id = ?",
                        String.class,
                        memberId));
    }

    /** 양쪽 회원만 해제 대상을 조회·삭제하고 두 회원의 개인 추천과 연결 원천 데이터만 제거하는지 검증한다. */
    @Test
    void disconnectsOnlyOwnedCoupleAndDeletesRelatedData() {
        Long inviterId = insertMember("disconnect-inviter");
        Long inviteeId = insertMember("disconnect-invitee");
        Long unrelatedMemberId = insertMember("disconnect-unrelated");
        Long commonSurveyId = insertCommonSurvey(inviterId);
        Long invitationCodeId = insertInvitationCode(commonSurveyId);
        Long coupleId = insertCouple(inviterId, inviteeId, invitationCodeId);
        Long productId = insertProduct();
        insertPersonalRecommendations(inviterId, productId);
        insertPersonalRecommendations(inviteeId, productId);
        insertPersonalRecommendations(unrelatedMemberId, productId);

        CoupleDisconnectTarget inviterTarget =
                coupleMapper.findDisconnectTargetByMemberId(inviterId);
        CoupleDisconnectTarget inviteeTarget =
                coupleMapper.findDisconnectTargetByMemberId(inviteeId);

        assertDisconnectTarget(
                inviterTarget,
                coupleId,
                inviterId,
                inviteeId,
                invitationCodeId,
                commonSurveyId);
        assertDisconnectTarget(
                inviteeTarget,
                coupleId,
                inviterId,
                inviteeId,
                invitationCodeId,
                commonSurveyId);
        assertNull(coupleMapper.findDisconnectTargetByMemberId(unrelatedMemberId));

        assertEquals(
                2,
                coupleMapper.deletePersonalTaxSavingByCoupleMembers(inviterId, inviteeId));
        assertEquals(
                2,
                coupleMapper.deletePersonalInvestmentByCoupleMembers(inviterId, inviteeId));
        assertEquals(1, countByMember("personal_recommendation_tax_saving", unrelatedMemberId));
        assertEquals(1, countByMember("personal_recommendation_investment", unrelatedMemberId));

        assertEquals(0, coupleMapper.deleteCoupleByIdAndMemberId(coupleId, unrelatedMemberId));
        assertEquals(1, coupleMapper.deleteCoupleByIdAndMemberId(coupleId, inviterId));
        assertEquals(1, coupleMapper.deleteInvitationById(invitationCodeId));
        assertEquals(1, coupleMapper.deleteCommonSurveyById(commonSurveyId));
        assertEquals(0, countById("couple", coupleId));
        assertEquals(0, countById("invitation_code", invitationCodeId));
        assertEquals(0, countById("common_survey", commonSurveyId));
    }

    private Long insertMember(String label) {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        String email = label + "-" + UUID.randomUUID() + "@mapper.test";
        jdbcTemplate.update(
                """
                INSERT INTO member (email, password, name, gender, birth_date)
                VALUES (?, ?, ?, ?, ?)
                """,
                email,
                "encoded-password",
                label,
                "F",
                Date.valueOf(LocalDate.of(1995, 3, 21)));

        return jdbcTemplate.queryForObject(
                "SELECT id FROM member WHERE email = ?",
                Long.class,
                email);
    }

    private Long insertCommonSurvey(Long memberId) {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        jdbcTemplate.update(
                """
                INSERT INTO common_survey (
                    member_id,
                    first_goal_type,
                    second_goal_type,
                    target_amount,
                    target_period_months,
                    loan_purpose,
                    has_loan_within_one_month
                ) VALUES (?, 'HOUSING', 'MARRIAGE', 300000000, 36, 'NONE', false)
                """,
                memberId);

        return jdbcTemplate.queryForObject(
                "SELECT id FROM common_survey WHERE member_id = ?",
                Long.class,
                memberId);
    }

    private Long insertInvitationCode(Long commonSurveyId) {
        return insertInvitationCode(commonSurveyId, invitationCode());
    }

    private Long insertInvitationCode(Long commonSurveyId, String codeValue) {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        jdbcTemplate.update(
                """
                INSERT INTO invitation_code (common_survey_id, code_value, status)
                VALUES (?, ?, 'ACTIVE')
                """,
                commonSurveyId,
                codeValue);

        return jdbcTemplate.queryForObject(
                "SELECT id FROM invitation_code WHERE code_value = ?",
                Long.class,
                codeValue);
    }

    private Long insertCouple(Long inviterId, Long inviteeId, Long invitationCodeId) {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        jdbcTemplate.update(
                """
                INSERT INTO couple (inviter_id, invitee_id, invitation_code_id)
                VALUES (?, ?, ?)
                """,
                inviterId,
                inviteeId,
                invitationCodeId);

        return jdbcTemplate.queryForObject(
                "SELECT id FROM couple WHERE invitation_code_id = ?",
                Long.class,
                invitationCodeId);
    }

    private Long insertPersonalSurvey(Long memberId) {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        jdbcTemplate.update(
                """
                INSERT INTO personal_survey (
                    member_id,
                    annual_income,
                    monthly_available_amount,
                    financial_asset_ratio,
                    financial_knowledge,
                    capital_preservation_attitude
                ) VALUES (?, 50000000, 1000000, 'UNDER_50', 'MEDIUM', 'UNDER_50')
                """,
                memberId);

        return jdbcTemplate.queryForObject(
                "SELECT id FROM personal_survey WHERE member_id = ?",
                Long.class,
                memberId);
    }

    private void insertInvestmentExperiences(Long personalSurveyId) {
        jdbcTemplate().update(
                """
                INSERT INTO personal_survey_investment_experience (
                    personal_survey_id,
                    investment_experience
                ) VALUES (?, 'LOW_RISK'), (?, 'MODERATE_RISK')
                """,
                personalSurveyId,
                personalSurveyId);
    }

    private Long insertProduct() {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        String productName = "mapper-product-" + UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO product (
                    product_type,
                    product_name,
                    description,
                    url,
                    company_name
                ) VALUES ('INVESTMENT', ?, '테스트 상품', 'https://example.com', '테스트 금융사')
                """,
                productName);

        return jdbcTemplate.queryForObject(
                "SELECT id FROM product WHERE product_name = ?",
                Long.class,
                productName);
    }

    private void insertPersonalRecommendations(Long memberId, Long productId) {
        jdbcTemplate().update(
                """
                INSERT INTO personal_recommendation_tax_saving (member_id, product_id, rank_no)
                VALUES (?, ?, 1)
                """,
                memberId,
                productId);
        jdbcTemplate().update(
                """
                INSERT INTO personal_recommendation_investment (member_id, product_id, rank_no)
                VALUES (?, ?, 1)
                """,
                memberId,
                productId);
    }

    private void assertProfile(
            CoupleProfile profile,
            Long coupleId,
            String myName,
            String partnerName,
            String profileMessage) {
        assertNotNull(profile);
        assertEquals(coupleId, profile.getCoupleId());
        assertEquals(myName, profile.getMyName());
        assertEquals(partnerName, profile.getPartnerName());
        assertEquals(profileMessage, profile.getProfileMessage());
    }

    private void assertDisconnectTarget(
            CoupleDisconnectTarget target,
            Long coupleId,
            Long inviterId,
            Long inviteeId,
            Long invitationCodeId,
            Long commonSurveyId) {
        assertNotNull(target);
        assertEquals(coupleId, target.getCoupleId());
        assertEquals(inviterId, target.getInviterId());
        assertEquals(inviteeId, target.getInviteeId());
        assertEquals(invitationCodeId, target.getInvitationCodeId());
        assertEquals(commonSurveyId, target.getCommonSurveyId());
    }

    private int countByMember(String tableName, Long memberId) {
        return count(
                "SELECT COUNT(*) FROM " + tableName + " WHERE member_id = ?",
                memberId);
    }

    private int countByPersonalSurveyId(String tableName, Long personalSurveyId) {
        return count(
                "SELECT COUNT(*) FROM " + tableName + " WHERE personal_survey_id = ?",
                personalSurveyId);
    }

    private int countById(String tableName, Long id) {
        return count(
                "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?",
                id);
    }

    private int count(String sql, Long id) {
        Integer count = jdbcTemplate().queryForObject(sql, Integer.class, id);
        return count == null ? 0 : count;
    }

    private String invitationCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
