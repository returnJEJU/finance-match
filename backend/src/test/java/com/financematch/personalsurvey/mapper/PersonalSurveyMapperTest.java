package com.financematch.personalsurvey.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.config.RootConfig;
import com.financematch.invitation.domain.GoalType;
import com.financematch.personalsurvey.domain.CoupleInvestmentType;
import com.financematch.personalsurvey.domain.InvestmentExperience;
import com.financematch.personalsurvey.domain.PersonalInvestmentType;
import com.financematch.personalsurvey.domain.PersonalSurvey;
import com.financematch.personalsurvey.domain.PersonalSurveyCalculationContext;
import com.financematch.personalsurvey.domain.PersonalSurveyResult;
import com.financematch.personalsurvey.domain.ReadyCoupleInvestmentTypes;
import com.financematch.personalsurvey.dto.PersonalSurveyRequest;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Set;
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
class PersonalSurveyMapperTest {

    @Autowired
    private PersonalSurveyMapper personalSurveyMapper;

    @Autowired
    private DataSource dataSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 활성 회원만 잠금 조회되고 탈퇴 회원과 존재하지 않는 회원은 조회되지 않는지 검증한다. */
    @Test
    void locksOnlyActiveMember() {
        Long activeMemberId = insertMember("active-member", LocalDate.of(1995, 3, 21));
        Long withdrawnMemberId = insertMember("withdrawn-member", LocalDate.of(1990, 1, 1));
        jdbcTemplate().update(
                "UPDATE member SET status = 'WITHDRAWN', deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                withdrawnMemberId);

        assertEquals(activeMemberId, personalSurveyMapper.lockActiveMemberById(activeMemberId));
        assertNull(personalSurveyMapper.lockActiveMemberById(withdrawnMemberId));
        assertNull(personalSurveyMapper.lockActiveMemberById(-1L));
    }

    /** 커플 연결 전에는 본인 공동 설문을, 연결 후에는 커플 생성에 사용된 공동 설문을 계산 정보로 조회하는지 검증한다. */
    @Test
    void findsCalculationContextFromOwnOrCoupleSurvey() {
        Long standaloneMemberId =
                insertMember("standalone-member", LocalDate.of(1995, 3, 21));
        insertCommonSurvey(standaloneMemberId, GoalType.HOUSING, 12);

        Long inviterId = insertMember("context-inviter", LocalDate.of(1990, 4, 10));
        Long inviteeId = insertMember("context-invitee", LocalDate.of(1992, 5, 15));
        Long coupleSurveyId = insertCommonSurvey(inviterId, GoalType.RETIREMENT, 60);
        insertCommonSurvey(inviteeId, GoalType.MARRIAGE, 6);
        Long invitationCodeId = insertInvitationCode(coupleSurveyId);
        insertCouple(inviterId, inviteeId, invitationCodeId);

        PersonalSurveyCalculationContext ownContext =
                personalSurveyMapper.findCalculationContext(standaloneMemberId);
        PersonalSurveyCalculationContext coupleContext =
                personalSurveyMapper.findCalculationContext(inviteeId);

        assertCalculationContext(
                ownContext,
                LocalDate.of(1995, 3, 21),
                GoalType.HOUSING,
                12);
        assertCalculationContext(
                coupleContext,
                LocalDate.of(1992, 5, 15),
                GoalType.RETIREMENT,
                60);
    }

    /** 개인 설문 INSERT가 생성 ID와 모든 필드를 저장하고 ID와 회원이 모두 일치할 때만 수정하는지 검증한다. */
    @Test
    void insertsAndUpdatesOnlyOwnedPersonalSurvey() throws Exception {
        Long memberId = insertMember("survey-owner", LocalDate.of(1995, 3, 21));
        Long otherMemberId = insertMember("survey-other", LocalDate.of(1993, 1, 1));
        PersonalSurvey survey = PersonalSurvey.of(memberId, personalSurveyRequest(
                45_000_000,
                1_000_000,
                "UNDER_50",
                "MEDIUM",
                "UNDER_50"));

        int inserted = personalSurveyMapper.insertPersonalSurvey(survey);

        assertEquals(1, inserted);
        assertNotNull(survey.getId());
        assertEquals(
                survey.getId(),
                personalSurveyMapper.findPersonalSurveyIdByMemberId(memberId));
        assertSurveyValues(
                survey.getId(),
                new BigDecimal("45000000"),
                new BigDecimal("1000000"),
                "UNDER_50",
                "MEDIUM",
                "UNDER_50");

        PersonalSurvey otherMemberSurvey = PersonalSurvey.of(
                otherMemberId,
                personalSurveyRequest(
                        80_000_000,
                        2_000_000,
                        "UNDER_80",
                        "HIGH",
                        "UNDER_20"));
        otherMemberSurvey.setId(survey.getId());
        assertEquals(0, personalSurveyMapper.updatePersonalSurvey(otherMemberSurvey));

        PersonalSurvey updatedSurvey = PersonalSurvey.of(
                memberId,
                personalSurveyRequest(
                        80_000_000,
                        2_000_000,
                        "UNDER_80",
                        "HIGH",
                        "UNDER_20"));
        updatedSurvey.setId(survey.getId());
        assertEquals(1, personalSurveyMapper.updatePersonalSurvey(updatedSurvey));
        assertSurveyValues(
                survey.getId(),
                new BigDecimal("80000000"),
                new BigDecimal("2000000"),
                "UNDER_80",
                "HIGH",
                "UNDER_20");
    }

    /** 여러 투자 경험이 한 번에 저장되고 해당 개인 설문의 경험만 모두 삭제되는지 검증한다. */
    @Test
    void insertsAndDeletesInvestmentExperiences() throws Exception {
        Long memberId = insertMember("experience-owner", LocalDate.of(1995, 3, 21));
        Long personalSurveyId = insertPersonalSurvey(memberId);
        Set<InvestmentExperience> experiences =
                Set.of(
                        InvestmentExperience.LOW_RISK,
                        InvestmentExperience.MODERATE_RISK,
                        InvestmentExperience.HIGH_RISK);

        assertEquals(
                experiences.size(),
                personalSurveyMapper.insertInvestmentExperiences(personalSurveyId, experiences));
        assertEquals(experiences.size(), countExperiences(personalSurveyId));
        assertEquals(
                Set.of("LOW_RISK", "MODERATE_RISK", "HIGH_RISK"),
                selectExperiences(personalSurveyId));

        assertEquals(
                experiences.size(),
                personalSurveyMapper.deleteInvestmentExperiences(personalSurveyId));
        assertEquals(0, countExperiences(personalSurveyId));
    }

    /** 두 회원의 투자성향이 모두 있을 때만 커플 계산 대상을 조회하고 계산 결과를 커플에 저장하는지 검증한다. */
    @Test
    void findsReadyCoupleAndUpdatesCoupleInvestmentType() {
        Long inviterId = insertMember("ready-inviter", LocalDate.of(1990, 1, 1));
        Long inviteeId = insertMember("ready-invitee", LocalDate.of(1992, 2, 2));
        Long commonSurveyId = insertCommonSurvey(inviterId, GoalType.INVESTMENT, 36);
        Long invitationCodeId = insertInvitationCode(commonSurveyId);
        Long coupleId = insertCouple(inviterId, inviteeId, invitationCodeId);

        assertNull(personalSurveyMapper.findReadyCoupleInvestmentTypes(inviterId));
        assertEquals(
                1,
                personalSurveyMapper.updateMemberInvestmentType(
                        inviterId,
                        PersonalInvestmentType.STABLE));
        assertNull(personalSurveyMapper.findReadyCoupleInvestmentTypes(inviteeId));
        assertEquals(
                1,
                personalSurveyMapper.updateMemberInvestmentType(
                        inviteeId,
                        PersonalInvestmentType.AGGRESSIVE));

        ReadyCoupleInvestmentTypes ready =
                personalSurveyMapper.findReadyCoupleInvestmentTypes(inviteeId);

        assertNotNull(ready);
        assertEquals(coupleId, ready.getCoupleId());
        assertEquals(PersonalInvestmentType.STABLE, ready.getInviterInvestmentType());
        assertEquals(PersonalInvestmentType.AGGRESSIVE, ready.getInviteeInvestmentType());
        assertEquals(
                1,
                personalSurveyMapper.updateCoupleInvestmentType(
                        coupleId,
                        CoupleInvestmentType.DIFF_3));
        assertEquals(
                CoupleInvestmentType.DIFF_3.name(),
                jdbcTemplate().queryForObject(
                        "SELECT investment_type FROM couple WHERE id = ?",
                        String.class,
                        coupleId));
    }

    /** 활성 회원의 이름과 개인 투자성향이 결과 객체에 매핑되고 탈퇴 회원은 조회되지 않는지 검증한다. */
    @Test
    void findsPersonalSurveyResultOnlyForActiveMember() {
        Long activeMemberId = insertMember("result-active", LocalDate.of(1995, 3, 21));
        Long withdrawnMemberId = insertMember("result-withdrawn", LocalDate.of(1990, 1, 1));
        assertEquals(
                1,
                personalSurveyMapper.updateMemberInvestmentType(
                        activeMemberId,
                        PersonalInvestmentType.NEUTRAL));
        jdbcTemplate().update(
                """
                UPDATE member
                SET investment_type = 'STABLE',
                    status = 'WITHDRAWN',
                    deleted_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                withdrawnMemberId);

        PersonalSurveyResult result =
                personalSurveyMapper.findPersonalSurveyResultByMemberId(activeMemberId);

        assertNotNull(result);
        assertEquals("result-active", result.getName());
        assertEquals(PersonalInvestmentType.NEUTRAL, result.getInvestmentType());
        assertNull(personalSurveyMapper.findPersonalSurveyResultByMemberId(withdrawnMemberId));
    }

    private Long insertMember(String label, LocalDate birthDate) {
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
                Date.valueOf(birthDate));

        return jdbcTemplate.queryForObject(
                "SELECT id FROM member WHERE email = ?",
                Long.class,
                email);
    }

    private Long insertCommonSurvey(Long memberId, GoalType firstGoalType, int targetPeriodMonths) {
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
                ) VALUES (?, ?, 'MARRIAGE', 300000000, ?, 'NONE', false)
                """,
                memberId,
                firstGoalType.name(),
                targetPeriodMonths);

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

    private Long insertPersonalSurvey(Long memberId) throws Exception {
        PersonalSurvey survey = PersonalSurvey.of(
                memberId,
                personalSurveyRequest(
                        50_000_000,
                        1_000_000,
                        "UNDER_50",
                        "MEDIUM",
                        "UNDER_50"));
        personalSurveyMapper.insertPersonalSurvey(survey);
        return survey.getId();
    }

    private PersonalSurveyRequest personalSurveyRequest(
            long annualIncome,
            long monthlyAvailableAmount,
            String financialAssetRatio,
            String financialKnowledge,
            String capitalPreservationAttitude) throws Exception {
        return objectMapper.readValue(
                """
                {
                  "annualIncome": %d,
                  "monthlyAvailableAmount": %d,
                  "financialAssetRatio": "%s",
                  "investmentExperiences": ["LOW_RISK"],
                  "financialKnowledge": "%s",
                  "capitalPreservationAttitude": "%s"
                }
                """.formatted(
                        annualIncome,
                        monthlyAvailableAmount,
                        financialAssetRatio,
                        financialKnowledge,
                        capitalPreservationAttitude),
                PersonalSurveyRequest.class);
    }

    private void assertCalculationContext(
            PersonalSurveyCalculationContext context,
            LocalDate birthDate,
            GoalType firstGoalType,
            int targetPeriodMonths) {
        assertNotNull(context);
        assertEquals(birthDate, context.getBirthDate());
        assertEquals(firstGoalType, context.getFirstGoalType());
        assertEquals(targetPeriodMonths, context.getTargetPeriodMonths());
    }

    private void assertSurveyValues(
            Long personalSurveyId,
            BigDecimal annualIncome,
            BigDecimal monthlyAvailableAmount,
            String financialAssetRatio,
            String financialKnowledge,
            String capitalPreservationAttitude) {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        assertEquals(
                annualIncome,
                jdbcTemplate.queryForObject(
                        "SELECT annual_income FROM personal_survey WHERE id = ?",
                        BigDecimal.class,
                        personalSurveyId));
        assertEquals(
                monthlyAvailableAmount,
                jdbcTemplate.queryForObject(
                        "SELECT monthly_available_amount FROM personal_survey WHERE id = ?",
                        BigDecimal.class,
                        personalSurveyId));
        assertEquals(
                financialAssetRatio,
                selectSurveyString("financial_asset_ratio", personalSurveyId));
        assertEquals(
                financialKnowledge,
                selectSurveyString("financial_knowledge", personalSurveyId));
        assertEquals(
                capitalPreservationAttitude,
                selectSurveyString("capital_preservation_attitude", personalSurveyId));
    }

    private String selectSurveyString(String columnName, Long personalSurveyId) {
        return jdbcTemplate().queryForObject(
                "SELECT " + columnName + " FROM personal_survey WHERE id = ?",
                String.class,
                personalSurveyId);
    }

    private Set<String> selectExperiences(Long personalSurveyId) {
        return Set.copyOf(
                jdbcTemplate().queryForList(
                        """
                        SELECT investment_experience
                        FROM personal_survey_investment_experience
                        WHERE personal_survey_id = ?
                        """,
                        String.class,
                        personalSurveyId));
    }

    private int countExperiences(Long personalSurveyId) {
        Integer count = jdbcTemplate().queryForObject(
                """
                SELECT COUNT(*)
                FROM personal_survey_investment_experience
                WHERE personal_survey_id = ?
                """,
                Integer.class,
                personalSurveyId);
        return count == null ? 0 : count;
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
