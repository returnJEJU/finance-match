package com.financematch.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.auth.dto.LoginRequest;
import com.financematch.config.RootConfig;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
class AuthServiceIntegrationTest {

    private static final Long MEMBER_ID = 1L;
    private static final LocalDateTime ORIGINAL_LOGIN_AT =
            LocalDateTime.of(2020, 1, 1, 0, 0);
    private static final LocalDateTime RECOMMENDATION_INPUT_UPDATED_AT =
            LocalDateTime.of(2026, 1, 1, 0, 0);

    @Autowired
    private AuthService authService;

    @Autowired
    private DataSource dataSource;

    @Test
    void 로그인은_추천입력_수정시각을_변경하지_않는다() throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                """
                UPDATE member
                SET last_login_at = ?,
                    recommendation_input_updated_at = ?
                WHERE id = ?
                """,
                ORIGINAL_LOGIN_AT,
                RECOMMENDATION_INPUT_UPDATED_AT,
                MEMBER_ID);

        LoginRequest request =
                new ObjectMapper()
                        .readValue(
                                """
                                {
                                  "email": "demo.a@chaltteok.dev",
                                  "password": "Test1234!"
                                }
                                """,
                                LoginRequest.class);

        authService.login(request);

        LocalDateTime updatedLoginAt =
                selectDateTime(jdbcTemplate, "last_login_at");
        LocalDateTime recommendationInputUpdatedAt =
                selectDateTime(jdbcTemplate, "recommendation_input_updated_at");

        assertNotEquals(ORIGINAL_LOGIN_AT, updatedLoginAt);
        assertEquals(RECOMMENDATION_INPUT_UPDATED_AT, recommendationInputUpdatedAt);
    }

    private LocalDateTime selectDateTime(JdbcTemplate jdbcTemplate, String columnName) {
        Timestamp timestamp =
                jdbcTemplate.queryForObject(
                        "SELECT " + columnName + " FROM member WHERE id = ?",
                        Timestamp.class,
                        MEMBER_ID);
        return timestamp.toLocalDateTime();
    }
}
