package com.financematch.couple.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.common.ErrorCode;
import com.financematch.config.RootConfig;
import com.financematch.couple.dto.CoupleProfileMessageResponse;
import com.financematch.exception.ApiException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@WebAppConfiguration
@Transactional
class CoupleServiceTest {

    @Autowired
    private CoupleService coupleService;

    @Autowired
    private DataSource dataSource;

    @Test
    void 한_명이_수정한_커플_한줄소개를_파트너도_조회한다() {

        // given
        String profileMessage = "함께 모으고 함께 투자하기";

        // when
        CoupleProfileMessageResponse updated =
                coupleService.updateProfileMessage(
                        1L,
                        profileMessage);
        CoupleProfileMessageResponse partnerView =
                coupleService.getProfileMessage(2L);

        // then
        assertEquals("김하나", updated.getMyName());
        assertEquals("이두리", updated.getPartnerName());
        assertEquals(profileMessage, updated.getProfileMessage());
        assertEquals("이두리", partnerView.getMyName());
        assertEquals("김하나", partnerView.getPartnerName());
        assertEquals(profileMessage, partnerView.getProfileMessage());
    }

    @Test
    void 연결된_커플이_없으면_한줄소개를_조회할_수_없다() {

        // when
        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.getProfileMessage(9999L));

        // then
        assertEquals(ErrorCode.COUPLE_NOT_CONNECTED, exception.getErrorCode());
    }

    @Test
    void 커플_연결을_끊으면_연결_데이터와_양쪽_개인설문이_삭제된다() {

        // given
        assertEquals(
                2,
                count(
                        "SELECT COUNT(*) FROM personal_survey "
                                + "WHERE member_id IN (1, 2)"));
        assertEquals(
                2,
                count(
                        "SELECT COUNT(*) FROM member "
                                + "WHERE id IN (1, 2) "
                                + "AND investment_type IS NOT NULL"));

        // when
        coupleService.disconnectCouple(1L);

        // then
        assertEquals(0, count("SELECT COUNT(*) FROM couple WHERE id = 1"));
        assertEquals(0, count("SELECT COUNT(*) FROM compatibility_result WHERE couple_id = 1"));
        assertEquals(0, count("SELECT COUNT(*) FROM report WHERE compatibility_result_id = 1"));
        assertEquals(0, count("SELECT COUNT(*) FROM recommendation WHERE couple_id = 1"));
        assertEquals(
                0,
                count(
                        "SELECT COUNT(*) FROM personal_recommendation_tax_saving "
                                + "WHERE member_id IN (1, 2)"));
        assertEquals(
                0,
                count(
                        "SELECT COUNT(*) FROM personal_recommendation_investment "
                                + "WHERE member_id IN (1, 2)"));
        assertEquals(0, count("SELECT COUNT(*) FROM invitation_code WHERE id = 1"));
        assertEquals(0, count("SELECT COUNT(*) FROM common_survey WHERE id = 1"));

        assertEquals(
                0,
                count(
                        "SELECT COUNT(*) FROM personal_survey "
                                + "WHERE member_id IN (1, 2)"));
        assertEquals(
                0,
                count(
                        "SELECT COUNT(*) FROM personal_survey_investment_experience "
                                + "WHERE personal_survey_id IN (1, 2)"));
        assertEquals(
                0,
                count(
                        "SELECT COUNT(*) FROM member "
                                + "WHERE id IN (1, 2) "
                                + "AND investment_type IS NOT NULL"));
    }

    @Test
    void 연결된_커플이_없으면_커플_연결을_끊을_수_없다() {

        // when
        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.disconnectCouple(9999L));

        // then
        assertEquals(ErrorCode.COUPLE_NOT_CONNECTED, exception.getErrorCode());
    }

    @Test
    void 탈퇴용_커플_해제는_연결된_커플이_없어도_통과한다() {

        // when
        assertDoesNotThrow(() -> coupleService.disconnectCoupleIfConnected(9999L));

        // then
        assertEquals(1, count("SELECT COUNT(*) FROM couple WHERE id = 1"));
    }

    private int count(String sql) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }
}
