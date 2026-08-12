package com.financematch.invitation.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.config.RootConfig;
import com.financematch.invitation.domain.CommonSurvey;
import com.financematch.invitation.domain.CommonSurveyLoanPurpose;
import com.financematch.invitation.domain.CommonSurveyUpdateTarget;
import com.financematch.invitation.domain.GoalType;
import com.financematch.invitation.dto.CommonSurveyResponse;
import com.financematch.invitation.dto.CreateInvitationRequest;
import com.financematch.invitation.dto.UpdateCommonSurveyRequest;
import java.math.BigDecimal;
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
class InvitationMapperTest {

    @Autowired
    private InvitationMapper invitationMapper;

    @Autowired
    private DataSource dataSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 활성 회원만 잠금 조회되고 탈퇴 회원과 존재하지 않는 회원은 조회되지 않는지 검증한다. */
    @Test
    void locksOnlyActiveMember() {
        Long activeMemberId = insertMember("active-member");
        Long withdrawnMemberId = insertMember("withdrawn-member");
        jdbcTemplate().update(
                "UPDATE member SET status = 'WITHDRAWN', deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                withdrawnMemberId);

        assertEquals(activeMemberId, invitationMapper.lockMemberById(activeMemberId));
        assertNull(invitationMapper.lockMemberById(withdrawnMemberId));
        assertNull(invitationMapper.lockMemberById(-1L));
    }

    /** 공동 설문을 저장하면 생성된 ID가 객체에 채워지고 모든 요청 필드가 DB에 저장되는지 검증한다. */
    @Test
    void insertsCommonSurveyAndSetsGeneratedId() throws Exception {
        Long memberId = insertMember("survey-writer");
        CommonSurvey survey = CommonSurvey.of(memberId, createRequest());

        int inserted = invitationMapper.insertCommonSurvey(survey);

        assertEquals(1, inserted);
        assertNotNull(survey.getId());
        assertEquals(
                memberId,
                jdbcTemplate().queryForObject(
                        "SELECT member_id FROM common_survey WHERE id = ?",
                        Long.class,
                        survey.getId()));
        assertEquals(
                GoalType.HOUSING.name(),
                selectString("first_goal_type", survey.getId()));
        assertEquals(
                GoalType.MARRIAGE.name(),
                selectString("second_goal_type", survey.getId()));
        assertEquals(
                new BigDecimal("300000000"),
                jdbcTemplate().queryForObject(
                        "SELECT target_amount FROM common_survey WHERE id = ?",
                        BigDecimal.class,
                        survey.getId()));
        assertEquals(
                36,
                jdbcTemplate().queryForObject(
                        "SELECT target_period_months FROM common_survey WHERE id = ?",
                        Integer.class,
                        survey.getId()));
        assertEquals(
                CommonSurveyLoanPurpose.NONE.name(),
                selectString("loan_purpose", survey.getId()));
        assertFalse(
                jdbcTemplate().queryForObject(
                        "SELECT has_loan_within_one_month FROM common_survey WHERE id = ?",
                        Boolean.class,
                        survey.getId()));
    }

    /** 활성 초대코드만 조회되며 같은 코드를 다시 저장하면 INSERT IGNORE가 0을 반환하는지 검증한다. */
    @Test
    void findsOnlyActiveInvitationAndIgnoresDuplicateCode() {
        Long memberId = insertMember("invitation-owner");
        Long commonSurveyId = insertCommonSurvey(memberId);
        String codeValue = invitationCode();

        assertEquals(1, invitationMapper.insertInvitationCode(commonSurveyId, codeValue));
        assertEquals(codeValue, invitationMapper.findActiveInviteCodeByMemberId(memberId));
        assertTrue(invitationMapper.existsActiveInvitationByMemberId(memberId));

        Long anotherMemberId = insertMember("another-invitation-owner");
        Long anotherCommonSurveyId = insertCommonSurvey(anotherMemberId);
        assertEquals(0, invitationMapper.insertInvitationCode(anotherCommonSurveyId, codeValue));

        jdbcTemplate().update(
                "UPDATE invitation_code SET status = 'USED' WHERE common_survey_id = ?",
                commonSurveyId);

        assertNull(invitationMapper.findActiveInviteCodeByMemberId(memberId));
        assertFalse(invitationMapper.existsActiveInvitationByMemberId(memberId));
    }

    /** 초대한 회원과 초대받은 회원은 커플로 조회되고 관계없는 회원은 조회되지 않는지 검증한다. */
    @Test
    void detectsCoupleForBothMembers() {
        Long inviterId = insertMember("couple-inviter");
        Long inviteeId = insertMember("couple-invitee");
        Long unrelatedMemberId = insertMember("unrelated-member");
        Long commonSurveyId = insertCommonSurvey(inviterId);
        Long invitationCodeId = insertInvitationCode(commonSurveyId);
        insertCouple(inviterId, inviteeId, invitationCodeId);

        assertTrue(invitationMapper.existsCoupleByMemberId(inviterId));
        assertTrue(invitationMapper.existsCoupleByMemberId(inviteeId));
        assertFalse(invitationMapper.existsCoupleByMemberId(unrelatedMemberId));
    }

    /** 공동 설문 작성자와 연결된 초대받은 회원만 같은 공동 설문을 조회할 수 있는지 검증한다. */
    @Test
    void allowsOnlyCoupleMembersToReadCommonSurvey() {
        Long inviterId = insertMember("read-inviter");
        Long inviteeId = insertMember("read-invitee");
        Long unrelatedMemberId = insertMember("read-unrelated");
        Long commonSurveyId = insertCommonSurvey(inviterId);
        Long invitationCodeId = insertInvitationCode(commonSurveyId);
        insertCouple(inviterId, inviteeId, invitationCodeId);

        CommonSurveyResponse inviterResponse =
                invitationMapper.findCommonSurveyByAccessibleMemberId(inviterId);
        CommonSurveyResponse inviteeResponse =
                invitationMapper.findCommonSurveyByAccessibleMemberId(inviteeId);

        assertCommonSurvey(inviterResponse);
        assertCommonSurvey(inviteeResponse);
        assertNull(invitationMapper.findCommonSurveyByAccessibleMemberId(unrelatedMemberId));
    }

    /** 커플 구성원만 수정 대상을 찾고 공동 설문의 모든 필드를 수정할 수 있는지 검증한다. */
    @Test
    void findsAccessibleUpdateTargetAndUpdatesCommonSurvey() throws Exception {
        Long inviterId = insertMember("update-inviter");
        Long inviteeId = insertMember("update-invitee");
        Long unrelatedMemberId = insertMember("update-unrelated");
        Long commonSurveyId = insertCommonSurvey(inviterId);
        Long invitationCodeId = insertInvitationCode(commonSurveyId);
        Long coupleId = insertCouple(inviterId, inviteeId, invitationCodeId);

        CommonSurveyUpdateTarget inviterTarget =
                invitationMapper.findCommonSurveyUpdateTargetForUpdate(inviterId);
        CommonSurveyUpdateTarget inviteeTarget =
                invitationMapper.findCommonSurveyUpdateTargetForUpdate(inviteeId);

        assertUpdateTarget(inviterTarget, commonSurveyId, coupleId);
        assertUpdateTarget(inviteeTarget, commonSurveyId, coupleId);
        assertNull(invitationMapper.findCommonSurveyUpdateTargetForUpdate(unrelatedMemberId));

        int updated = invitationMapper.updateCommonSurvey(commonSurveyId, updateRequest());

        assertEquals(1, updated);
        CommonSurveyResponse response =
                invitationMapper.findCommonSurveyByAccessibleMemberId(inviteeId);
        assertEquals(GoalType.RETIREMENT, response.getGoalType1());
        assertEquals(GoalType.INVESTMENT, response.getGoalType2());
        assertEquals(new BigDecimal("500000000"), response.getTargetAmount());
        assertEquals(60, response.getTargetPeriodMonths());
        assertEquals(CommonSurveyLoanPurpose.HOUSING, response.getLoanPurpose());
        assertTrue(response.getHasLoanWithinOneMonth());
    }

    /** 공동 설문 변경 시 지정한 커플의 기존 궁합도와 추천 결과를 삭제할 수 있는지 검증한다. */
    @Test
    void deletesDerivedResultsForCouple() {
        Long inviterId = insertMember("delete-inviter");
        Long inviteeId = insertMember("delete-invitee");
        Long commonSurveyId = insertCommonSurvey(inviterId);
        Long invitationCodeId = insertInvitationCode(commonSurveyId);
        Long coupleId = insertCouple(inviterId, inviteeId, invitationCodeId);
        insertCompatibilityResult(coupleId);
        insertRecommendation(coupleId);

        assertEquals(1, invitationMapper.deleteCompatibilityResultByCoupleId(coupleId));
        assertEquals(1, invitationMapper.deleteRecommendationByCoupleId(coupleId));
        assertEquals(0, count("compatibility_result", coupleId));
        assertEquals(0, count("recommendation", coupleId));
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
        String codeValue = invitationCode();
        invitationMapper.insertInvitationCode(commonSurveyId, codeValue);
        return jdbcTemplate().queryForObject(
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

    private void insertCompatibilityResult(Long coupleId) {
        jdbcTemplate().update(
                "INSERT INTO compatibility_result (couple_id) VALUES (?)",
                coupleId);
    }

    private void insertRecommendation(Long coupleId) {
        jdbcTemplate().update(
                "INSERT INTO recommendation (couple_id) VALUES (?)",
                coupleId);
    }

    private CreateInvitationRequest createRequest() throws Exception {
        return objectMapper.readValue(
                """
                {
                  "goalType1": "HOUSING",
                  "goalType2": "MARRIAGE",
                  "targetAmount": 300000000,
                  "targetPeriodMonths": 36,
                  "loanPurpose": "NONE",
                  "hasLoanWithinOneMonth": false
                }
                """,
                CreateInvitationRequest.class);
    }

    private UpdateCommonSurveyRequest updateRequest() throws Exception {
        return objectMapper.readValue(
                """
                {
                  "goalType1": "RETIREMENT",
                  "goalType2": "INVESTMENT",
                  "targetAmount": 500000000,
                  "targetPeriodMonths": 60,
                  "loanPurpose": "HOUSING",
                  "hasLoanWithinOneMonth": true
                }
                """,
                UpdateCommonSurveyRequest.class);
    }

    private void assertCommonSurvey(CommonSurveyResponse response) {
        assertNotNull(response);
        assertEquals(GoalType.HOUSING, response.getGoalType1());
        assertEquals(GoalType.MARRIAGE, response.getGoalType2());
        assertEquals(new BigDecimal("300000000"), response.getTargetAmount());
        assertEquals(36, response.getTargetPeriodMonths());
        assertEquals(CommonSurveyLoanPurpose.NONE, response.getLoanPurpose());
        assertFalse(response.getHasLoanWithinOneMonth());
    }

    private void assertUpdateTarget(
            CommonSurveyUpdateTarget target,
            Long commonSurveyId,
            Long coupleId) {
        assertNotNull(target);
        assertEquals(commonSurveyId, target.getCommonSurveyId());
        assertEquals(coupleId, target.getCoupleId());
    }

    private String selectString(String columnName, Long commonSurveyId) {
        return jdbcTemplate().queryForObject(
                "SELECT " + columnName + " FROM common_survey WHERE id = ?",
                String.class,
                commonSurveyId);
    }

    private int count(String tableName, Long coupleId) {
        Integer count = jdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE couple_id = ?",
                Integer.class,
                coupleId);
        return count == null ? 0 : count;
    }

    private String invitationCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
