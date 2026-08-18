package com.financematch.auth.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.financematch.auth.domain.Member;
import com.financematch.onboarding.dto.OnboardingStatusResponse;
import org.junit.jupiter.api.Test;

/**
 * 로그인 응답의 JSON 필드명 계약.
 *
 * <p>Lombok 이 {@code boolean isFirstLogin} 에 만드는 게터는 {@code isFirstLogin()} 이고, Jackson 은
 * boolean 게터의 {@code is} 접두사를 떼어내 {@code firstLogin} 으로 직렬화한다. 명세는 {@code
 * isFirstLogin} 이므로 어긋나면 프론트가 값을 못 찾는다. 컴파일러가 잡아주지 않아 테스트로 고정한다.
 */
class LoginResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void 첫_로그인_여부는_isFirstLogin_이라는_이름으로_직렬화된다() throws Exception {
        ObjectNode json = serialize(true);

        assertTrue(json.has("isFirstLogin"), "isFirstLogin 필드가 없다");
        assertTrue(json.get("isFirstLogin").asBoolean());
        assertFalse(json.has("firstLogin"), "is 가 떨어진 firstLogin 이 함께 나가면 안 된다");
    }

    @Test
    void 명세가_정한_다섯_필드가_모두_담긴다() throws Exception {
        ObjectNode json = serialize(false);

        assertEquals(5, json.size());
        assertTrue(json.has("accessToken"));
        // access 토큰이 만료됐을 때 재발급에 쓴다. 로그인 응답에서만 받을 수 있으므로 빠지면
        // 프론트가 1시간 뒤 로그인 화면으로 튕긴다.
        assertTrue(json.has("refreshToken"));
        assertTrue(json.has("member"));
        assertTrue(json.has("isFirstLogin"));
        assertTrue(json.has("progress"));
    }

    @Test
    void member_에는_id_와_name_만_담기고_이메일은_빠진다() throws Exception {
        ObjectNode member = (ObjectNode) serialize(false).get("member");

        assertEquals(2, member.size());
        assertTrue(member.has("id"));
        assertTrue(member.has("name"));
        assertFalse(member.has("email"), "이메일은 로그인 응답에 담지 않는다");
    }

    private ObjectNode serialize(boolean isFirstLogin) throws Exception {
        Member member =
                objectMapper.readValue(
                        "{\"id\":1,\"email\":\"hong@kb.com\",\"name\":\"홍길동\"}", Member.class);

        LoginResponse response =
                LoginResponse.of(
                        member,
                        "eyJ.test.token",
                        "eyJ.test.refresh",
                        isFirstLogin,
                        new OnboardingStatusResponse());

        return (ObjectNode) objectMapper.readTree(objectMapper.writeValueAsString(response));
    }
}
