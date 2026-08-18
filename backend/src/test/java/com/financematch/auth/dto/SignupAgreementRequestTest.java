package com.financematch.auth.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    /**
     * 필수 4종을 하나씩 돌아가며 미동의로 두고 확인한다.
     *
     * <p>한 항목만 대표로 검사하면 나머지 조건이 {@code &&} 단축 평가에 가려 한 번도 판정되지 않는다 —
     * 조건 순서를 잘못 바꾸거나 항목을 빠뜨려도 테스트가 통과해버린다.
     */
    @ParameterizedTest(name = "{0} 에 동의하지 않으면 false")
    @ValueSource(strings = {"mydataTerms", "privacy", "assetLink", "coupleShare"})
    void 필수_항목이_하나라도_false_면_false_다(String notAgreed) throws Exception {
        SignupAgreementRequest request = parse(agreementsWithout(notAgreed));

        assertFalse(request.hasAllRequiredAgreements());
    }

    /** 지정한 한 항목만 false 이고 나머지 필수 항목은 모두 true 인 요청 JSON. */
    private String agreementsWithout(String notAgreed) {
        return """
                { "mydataTerms": %b, "privacy": %b, "assetLink": %b,
                  "coupleShare": %b, "marketing": true }
                """
                .formatted(
                        !"mydataTerms".equals(notAgreed),
                        !"privacy".equals(notAgreed),
                        !"assetLink".equals(notAgreed),
                        !"coupleShare".equals(notAgreed));
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
