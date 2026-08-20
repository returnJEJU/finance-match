package com.financematch.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 헬스체크 엔드포인트 검증.
 *
 * <p>로직이 없는 엔드포인트지만 <b>경로와 응답 형식</b>은 계약이다. 배포 후 로드밸런서·모니터링이 이
 * 주소를 상태 확인 대상으로 삼기 때문에, 경로가 바뀌거나 응답 형식이 달라지면 <b>서버는 멀쩡한데
 * 죽은 것으로 판정</b>될 수 있다.
 *
 * <p>DispatcherServlet 이 {@code /api/*} 에 매핑돼 있어 톰캣이 {@code /api} 를 떼고 넘긴다. 컨트롤러는
 * {@code /health} 만 쓰고 실제 주소는 {@code /api/health} 가 된다 — 버전을 붙이지 않는 유일한 예외다.
 */
class HealthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new HealthController()).build();

    @Test
    void 헬스체크는_health_경로에서_ok_를_돌려준다() throws Exception {
        String content =
                mockMvc.perform(get("/health"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        JsonNode body = objectMapper.readTree(content);

        assertTrue(body.get("success").asBoolean());
        assertEquals("ok", body.get("data").asText());
    }
}
