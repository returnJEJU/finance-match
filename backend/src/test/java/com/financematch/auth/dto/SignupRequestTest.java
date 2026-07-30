package com.financematch.auth.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.financematch.auth.domain.Gender;
import java.time.LocalDate;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.jupiter.api.Test;

/**
 * 회원가입 요청 DTO 검증.
 *
 * <p>실제 요청 경로와 같게 JSON 문자열을 역직렬화한 뒤 검증한다. {@code ObjectMapper} 설정은 {@code
 * WebConfig} 와 동일하게 {@code JavaTimeModule} 만 등록한다.
 */
class SignupRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static final String VALID_JSON =
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
            """;

    @Test
    void 정상_요청은_검증을_통과한다() throws Exception {
        SignupRequest request = objectMapper.readValue(VALID_JSON, SignupRequest.class);

        assertTrue(validate(request).isEmpty());
    }

    @Test
    void 문자열이_성별_enum_과_생년월일_타입으로_변환된다() throws Exception {
        SignupRequest request = objectMapper.readValue(VALID_JSON, SignupRequest.class);

        assertEquals(Gender.F, request.getGender());
        assertEquals(LocalDate.of(1995, 3, 21), request.getBirthDate());
    }

    @Test
    void 정의되지_않은_성별_값은_역직렬화_단계에서_실패한다() {
        // 컨트롤러에 들어오기 전에 터지므로 @Valid 로는 잡을 수 없다.
        // 실제 요청에서는 Spring 이 HttpMessageNotReadableException 으로 감싸 400 INVALID_INPUT 이 된다.
        String json = VALID_JSON.replace("\"gender\": \"F\"", "\"gender\": \"X\"");

        assertThrows(
                InvalidFormatException.class,
                () -> objectMapper.readValue(json, SignupRequest.class));
    }

    @Test
    void 이메일_형식이_아니면_검증에_실패한다() throws Exception {
        SignupRequest request = readWith("\"email\": \"hong@kb.com\"", "\"email\": \"hong\"");

        assertEquals("이메일 형식이 올바르지 않습니다.", firstMessage(request));
    }

    @Test
    void 비밀번호가_8자보다_짧으면_검증에_실패한다() throws Exception {
        SignupRequest request = readWith("\"password\": \"Pw123456!\"", "\"password\": \"Pw12345\"");

        assertEquals("비밀번호는 8자 이상 64자 이하여야 합니다.", firstMessage(request));
    }

    @Test
    void 생년월일이_미래이면_검증에_실패한다() throws Exception {
        SignupRequest request =
                readWith("\"birthDate\": \"1995-03-21\"", "\"birthDate\": \"2999-01-01\"");

        assertEquals("생년월일이 올바르지 않습니다.", firstMessage(request));
    }

    @Test
    void 약관_객체가_비어_있으면_중첩_검증이_동작한다() throws Exception {
        // agreements 필드의 @Valid 가 없으면 이 요청이 그대로 통과해 서비스에서 null 을 만난다.
        SignupRequest request =
                objectMapper.readValue(
                        VALID_JSON.replaceAll("(?s)\"agreements\": \\{.*?\\}", "\"agreements\": {}"),
                        SignupRequest.class);

        Set<ConstraintViolation<SignupRequest>> violations = validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().allMatch(v -> v.getPropertyPath().toString().startsWith("agreements.")));
    }

    private SignupRequest readWith(String target, String replacement) throws Exception {
        return objectMapper.readValue(VALID_JSON.replace(target, replacement), SignupRequest.class);
    }

    private Set<ConstraintViolation<SignupRequest>> validate(SignupRequest request) {
        return validator.validate(request);
    }

    private String firstMessage(SignupRequest request) {
        return validate(request).iterator().next().getMessage();
    }
}
