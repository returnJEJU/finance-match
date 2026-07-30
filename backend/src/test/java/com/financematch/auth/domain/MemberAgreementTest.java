package com.financematch.auth.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.auth.dto.SignupAgreementRequest;
import org.junit.jupiter.api.Test;

/** 약관 동의 요청 → 저장용 domain 변환. 불리언 5개가 뒤섞이지 않는지 확인한다. */
class MemberAgreementTest {

    private static final Long MEMBER_ID = 7L;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 각_동의_항목이_해당_컬럼_필드로_옮겨진다() throws Exception {
        // 항목별로 값을 다르게 두어 서로 바꿔 넣으면 실패하게 만든다.
        SignupAgreementRequest request =
                parse(
                        """
                        { "mydataTerms": true, "privacy": false, "assetLink": true,
                          "coupleShare": false, "marketing": true }
                        """);

        MemberAgreement agreement = MemberAgreement.of(MEMBER_ID, request);

        assertEquals(MEMBER_ID, agreement.getMemberId());
        assertTrue(agreement.getAgreeMydataTerms());
        assertFalse(agreement.getAgreePrivacy());
        assertTrue(agreement.getAgreeAssetLink());
        assertFalse(agreement.getAgreeCoupleShare());
        assertTrue(agreement.getAgreeMarketing());
    }

    @Test
    void 마케팅_동의가_오지_않으면_false_로_저장된다() throws Exception {
        // agree_marketing 은 NOT NULL 이라 null 을 그대로 넘기면 INSERT 가 실패한다.
        SignupAgreementRequest request =
                parse(
                        """
                        { "mydataTerms": true, "privacy": true, "assetLink": true,
                          "coupleShare": true }
                        """);

        MemberAgreement agreement = MemberAgreement.of(MEMBER_ID, request);

        assertFalse(agreement.getAgreeMarketing());
    }

    @Test
    void 동의_시각이_채워진다() throws Exception {
        // agreed_at 은 NOT NULL 이고 DB 기본값이 없어 코드에서 넣어야 한다.
        MemberAgreement agreement =
                MemberAgreement.of(
                        MEMBER_ID,
                        parse(
                                """
                                { "mydataTerms": true, "privacy": true, "assetLink": true,
                                  "coupleShare": true, "marketing": false }
                                """));

        assertNotNull(agreement.getAgreedAt());
    }

    private SignupAgreementRequest parse(String json) throws Exception {
        return objectMapper.readValue(json, SignupAgreementRequest.class);
    }
}
