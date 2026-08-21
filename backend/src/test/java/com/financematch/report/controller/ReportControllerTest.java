package com.financematch.report.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.auth.resolver.LoginMemberArgumentResolver;
import com.financematch.report.dto.ReportResponse;
import com.financematch.report.dto.ReportStatusResponse;
import com.financematch.report.service.ReportService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 리포트 조회 컨트롤러 검증.
 *
 * <p>이 컨트롤러는 서비스에 위임만 하므로 로직이랄 것이 없다. 그래서 <b>메서드를 직접 부르지 않고</b>
 * MockMvc 로 요청을 태운다 — 여기서 실제로 깨질 수 있는 것은 로직이 아니라 <b>경로 매핑과 회원 ID
 * 주입</b>이기 때문이다.
 *
 * <p>경로를 {@code /api/v1/...} 로 잘못 쓰는 실수는 AGENTS.md 에도 적혀 있다. DispatcherServlet 이
 * {@code /api/*} 에 매핑돼 있어 톰캣이 {@code /api} 를 떼고 넘기므로, 컨트롤러가 {@code /api} 를 다시
 * 쓰면 404 가 난다. 메서드를 직접 호출하는 테스트는 이 실수를 잡지 못한다.
 *
 * <p>{@code @LoginMember} 리졸버도 목이 아니라 실제 객체를 등록한다. "인증된 회원의 ID 가 서비스까지
 * 그대로 전달되는가" 가 이 계층의 핵심 계약이라서다.
 */
@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private static final Long MEMBER_ID = 42L;

    @Mock private ReportService reportService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new ReportController(reportService))
                        .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                        .build();

        // 리졸버는 토큰을 해석하지 않고 SecurityContext 에 담긴 값을 꺼내 쓴다(인증 필터가 채워둔 값).
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                MEMBER_ID, null, AuthorityUtils.NO_AUTHORITIES));
    }

    @AfterEach
    void tearDown() {
        // SecurityContextHolder 는 ThreadLocal 이다. 비우지 않으면 인증 상태가 다음 테스트로 샌다.
        SecurityContextHolder.clearContext();
    }

    @Test
    void 리포트는_members_me_경로에서_조회된다() throws Exception {
        when(reportService.getReport(MEMBER_ID)).thenReturn(sampleReport());

        JsonNode body = getJson("/v1/members/me/report");

        assertTrue(body.get("success").asBoolean());
        assertEquals("홍길동", body.get("data").get("name").asText());
        assertEquals("73.15", body.get("data").get("totalScore").asText());
    }

    @Test
    void 리포트_조회는_인증된_회원_ID_로_서비스를_호출한다() throws Exception {
        // 경로에 회원 식별자를 노출하지 않으므로(IDOR 차단), 대상 회원은 토큰에서만 정해져야 한다.
        when(reportService.getReport(MEMBER_ID)).thenReturn(sampleReport());

        mockMvc.perform(get("/v1/members/me/report")).andExpect(status().isOk());

        // 스텁이 MEMBER_ID 로만 걸려 있어, 다른 ID 로 불렸다면 null 이 돌아와 위 검증이 깨진다.
        verify(reportService).getReport(MEMBER_ID);
    }

    @Test
    void 진행_상태는_status_하위_경로에서_조회된다() throws Exception {
        when(reportService.getReportStatus(MEMBER_ID))
                .thenReturn(new ReportStatusResponse(false, 3, 5));

        JsonNode body = getJson("/v1/members/me/report/status");

        assertTrue(body.get("success").asBoolean());
        // 프론트가 진행 바를 그리려고 폴링하는 값이라 세 필드가 모두 나가야 한다.
        assertEquals(false, body.get("data").get("ready").asBoolean());
        assertEquals(3, body.get("data").get("completedAxes").asInt());
        assertEquals(5, body.get("data").get("totalAxes").asInt());
    }

    @Test
    void 준비가_끝나면_ready_가_참으로_나간다() throws Exception {
        when(reportService.getReportStatus(MEMBER_ID))
                .thenReturn(new ReportStatusResponse(true, 5, 5));

        JsonNode body = getJson("/v1/members/me/report/status");

        assertTrue(body.get("data").get("ready").asBoolean());
    }

    // ===== 도우미 =====

    private JsonNode getJson(String path) throws Exception {
        String content =
                mockMvc.perform(get(path))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readTree(content);
    }

    /** 응답 봉투와 경로만 확인하면 되므로 축·상세는 비워 둔다. */
    private ReportResponse sampleReport() {
        return new ReportResponse(
                "홍길동",
                "김영희",
                new BigDecimal("73.15"),
                List.of(),
                36,
                null,
                null,
                null,
                null);
    }
}
