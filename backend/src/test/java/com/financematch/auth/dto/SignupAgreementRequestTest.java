package com.financematch.auth.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** 필수 약관 4종 동의 판정. 이 판정이 false 면 {@code AuthService} 가 CONSENT_REQUIRED 를 던진다. */
class SignupAgreementRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 필수_4종에_모두_동의하면_true_다() throws Exception {
        SignupAgreementRequest request =
                parse(
                        """
                        { "mydataTerms": true, "privacy": true, "assetLink": true,
                          "coupleShare": true, "marketing": false }
                        """);

        assertTrue(request.hasAllRequiredAgreements());
    }

    @Test
    void 필수_항목이_하나라도_false_면_false_다() throws Exception {
        SignupAgreementRequest request =
                parse(
                        """
                        { "mydataTerms": true, "privacy": true, "assetLink": false,
                          "coupleShare": true, "marketing": true }
                        """);

        assertFalse(request.hasAllRequiredAgreements());
    }

    @Test
    void 필수_항목이_아예_오지_않아도_예외없이_false_다() throws Exception {
        // Boolean 은 null 이 될 수 있어 if (mydataTerms) 로 쓰면 NullPointerException 이 난다.
        SignupAgreementRequest request = parse("{ \"marketing\": true }");

        assertFalse(request.hasAllRequiredAgreements());
    }

    @Test
    void 마케팅_동의는_필수_판정에_영향을_주지_않는다() throws Exception {
        SignupAgreementRequest request =
                parse(
                        """
                        { "mydataTerms": true, "privacy": true, "assetLink": true,
                          "coupleShare": true }
                        """);

        assertTrue(request.hasAllRequiredAgreements());
    }

    private SignupAgreementRequest parse(String json) throws Exception {
        return objectMapper.readValue(json, SignupAgreementRequest.class);
    }
}
