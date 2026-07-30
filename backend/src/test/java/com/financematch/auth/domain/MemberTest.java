package com.financematch.auth.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.financematch.auth.dto.SignupRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** 회원가입 요청 → 저장용 domain 변환. 필드를 서로 바꿔 넣는 실수를 막는다. */
class MemberTest {

    private static final String ENCODED_PASSWORD = "$2a$10$abcdefghijklmnopqrstuv";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void 요청의_각_항목이_해당_필드로_옮겨진다() throws Exception {
        Member member = Member.of(request(), ENCODED_PASSWORD);

        assertEquals("hong@kb.com", member.getEmail());
        assertEquals("홍길동", member.getName());
        assertEquals(Gender.F, member.getGender());
        assertEquals(LocalDate.of(1995, 3, 21), member.getBirthDate());
    }

    @Test
    void 평문이_아니라_해시된_비밀번호가_저장된다() throws Exception {
        SignupRequest request = request();

        Member member = Member.of(request, ENCODED_PASSWORD);

        assertEquals(ENCODED_PASSWORD, member.getPassword());
        assertNotEquals(request.getPassword(), member.getPassword());
    }

    @Test
    void 변환_시점에는_id_가_없다() throws Exception {
        // id 는 AUTO_INCREMENT 라서 INSERT 후 MyBatis 가 채운다.
        Member member = Member.of(request(), ENCODED_PASSWORD);

        assertNull(member.getId());
    }

    private SignupRequest request() throws Exception {
        return objectMapper.readValue(
                """
                {
                  "name": "홍길동",
                  "gender": "F",
                  "birthDate": "1995-03-21",
                  "email": "hong@kb.com",
                  "password": "Pw123456!",
                  "agreements": {
                    "mydataTerms": true, "privacy": true, "assetLink": true,
                    "coupleShare": true, "marketing": false
                  }
                }
                """,
                SignupRequest.class);
    }
}
