package com.financematch.onboarding.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.financematch.config.RootConfig;
import com.financematch.onboarding.dto.OnboardingStatusResponse;
import java.sql.Date;
import java.time.LocalDate;
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
class OnboardingMapperTest {

    @Autowired
    private OnboardingMapper onboardingMapper;

    @Autowired
    private DataSource dataSource;

    /** 존재하지 않는 회원은 온보딩 상태를 조회할 수 없는지 검증한다. */
    @Test
    void returnsNullWhenMemberDoesNotExist() {
        OnboardingStatusResponse response = onboardingMapper.findOnboardingStatus(-1L);

        assertNull(response);
    }

    /** 가입 직후 회원은 커플·초대·개인 설문이 없고 파트너 설문 상태도 정해지지 않는지 검증한다. */
    @Test
    void returnsInitialStatusForNewMember() {
        Long memberId = insertMember("new-member");

        OnboardingStatusResponse response = onboardingMapper.findOnboardingStatus(memberId);

        assertNotNull(response);
        assertFalse(response.isCoupleConnected());
        assertFalse(response.isHasInvitation());
        assertFalse(response.isPersonalSurveyCompleted());
        assertNull(response.getPartnerPersonalSurveyCompleted());
    }

    /** 초대코드를 만들었지만 아직 연결되지 않은 회원은 초대 보유 상태만 완료되는지 검증한다. */
    @Test
    void returnsInvitationStatusBeforeCoupleConnection() {
        Long memberId = insertMember("inviter-before-connection");
        Long commonSurveyId = insertCommonSurvey(memberId);
        insertInvitationCode(commonSurveyId);

        OnboardingStatusResponse response = onboardingMapper.findOnboardingStatus(memberId);

        assertNotNull(response);
        assertFalse(response.isCoupleConnected());
        assertTrue(response.isHasInvitation());
        assertFalse(response.isPersonalSurveyCompleted());
        assertNull(response.getPartnerPersonalSurveyCompleted());
    }

    /** 초대한 회원만 개인 설문을 마치면 연결과 초대 상태는 완료되고 파트너 설문 상태는 false인지 검증한다. */
    @Test
    void returnsIncompletePartnerStatusForConnectedInviter() {
        Long inviterId = insertMember("connected-inviter");
        Long inviteeId = insertMember("connected-invitee");
        Long commonSurveyId = insertCommonSurvey(inviterId);
        Long invitationCodeId = insertInvitationCode(commonSurveyId);
        insertCouple(inviterId, inviteeId, invitationCodeId);
        insertPersonalSurvey(inviterId);

        OnboardingStatusResponse response = onboardingMapper.findOnboardingStatus(inviterId);

        assertNotNull(response);
        assertTrue(response.isCoupleConnected());
        assertTrue(response.isHasInvitation());
        assertTrue(response.isPersonalSurveyCompleted());
        assertFalse(response.getPartnerPersonalSurveyCompleted());
    }

    /** 두 회원 모두 개인 설문을 마치면 초대받은 회원에게 파트너 설문 완료 상태가 true인지 검증한다. */
    @Test
    void returnsCompletedPartnerStatusForConnectedInvitee() {
        Long inviterId = insertMember("completed-inviter");
        Long inviteeId = insertMember("completed-invitee");
        Long commonSurveyId = insertCommonSurvey(inviterId);
        Long invitationCodeId = insertInvitationCode(commonSurveyId);
        insertCouple(inviterId, inviteeId, invitationCodeId);
        insertPersonalSurvey(inviterId);
        insertPersonalSurvey(inviteeId);

        OnboardingStatusResponse response = onboardingMapper.findOnboardingStatus(inviteeId);

        assertNotNull(response);
        assertTrue(response.isCoupleConnected());
        assertFalse(response.isHasInvitation());
        assertTrue(response.isPersonalSurveyCompleted());
        assertTrue(response.getPartnerPersonalSurveyCompleted());
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
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        String codeValue = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
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

    private void insertCouple(Long inviterId, Long inviteeId, Long invitationCodeId) {
        jdbcTemplate().update(
                """
                INSERT INTO couple (inviter_id, invitee_id, invitation_code_id)
                VALUES (?, ?, ?)
                """,
                inviterId,
                inviteeId,
                invitationCodeId);
    }

    private void insertPersonalSurvey(Long memberId) {
        jdbcTemplate().update(
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
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
