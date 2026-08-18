package com.financematch.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.financematch.auth.domain.Member;
import com.financematch.auth.domain.MemberAgreement;
import com.financematch.auth.dto.LoginRequest;
import com.financematch.auth.dto.LoginResponse;
import com.financematch.auth.dto.SignupRequest;
import com.financematch.auth.dto.SignupResponse;
import com.financematch.auth.dto.TokenResponse;
import com.financematch.auth.jwt.JwtProvider;
import com.financematch.auth.jwt.RefreshTokenStore;
import com.financematch.auth.jwt.TokenBlacklist;
import com.financematch.auth.mapper.MemberMapper;
import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.onboarding.service.OnboardingService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long GENERATED_ID = 7L;
    private static final String RAW_PASSWORD = "Pw123456!";
    private static final String ENCODED_PASSWORD = "$2a$10$encoded-hash-value";
    private static final String ACCESS_TOKEN = "issued.access.token";
    private static final String REFRESH_TOKEN = "issued.refresh.token";
    private static final String EMAIL = "hong@kb.com";
    private static final String WRONG_PASSWORD = "WrongPw999!";

    /** 필수 4종에 모두 동의한 정상 요청. */
    private static final String VALID_SIGNUP_JSON =
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

    /** 필수 항목 하나(자산 궁합 공유)에 동의하지 않은 요청. */
    private static final String MISSING_CONSENT_JSON =
            """
            {
              "name": "홍길동",
              "gender": "F",
              "birthDate": "1995-03-21",
              "email": "hong@kb.com",
              "password": "Pw123456!",
              "agreements": {
                "mydataTerms": true, "privacy": true, "assetLink": true,
                "coupleShare": false, "marketing": true
              }
            }
            """;

    // DTO 에 setter 가 없어 실제 요청 경로와 같게 JSON 을 역직렬화해 만든다(SignupRequestTest 와 동일).
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock private MemberMapper memberMapper;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private JwtProvider jwtProvider;

    @Mock private OnboardingService onboardingService;

    @Mock private RefreshTokenStore refreshTokenStore;

    @Mock private TokenBlacklist tokenBlacklist;

    @InjectMocks private AuthService authService;

    // ===== 회원가입 =====

    @Test
    void 가입에_성공하면_회원정보와_발급된_토큰을_돌려준다() throws Exception {
        givenSignupSucceeds();

        SignupResponse response = authService.signup(request(VALID_SIGNUP_JSON));

        assertEquals(ACCESS_TOKEN, response.getAccessToken());
        // insert 후 채워진 자동 생성 PK 가 응답까지 전달되어야 한다.
        assertEquals(GENERATED_ID, response.getMember().getId());
        assertEquals("hong@kb.com", response.getMember().getEmail());
        assertEquals("홍길동", response.getMember().getName());
    }

    @Test
    void 비밀번호는_평문이_아니라_해시로_저장한다() throws Exception {
        givenSignupSucceeds();

        authService.signup(request(VALID_SIGNUP_JSON));

        ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).insert(saved.capture());

        assertEquals(ENCODED_PASSWORD, saved.getValue().getPassword());
        assertNotEquals(RAW_PASSWORD, saved.getValue().getPassword());
    }

    @Test
    void 약관_동의_내역을_회원과_함께_저장한다() throws Exception {
        givenSignupSucceeds();

        authService.signup(request(VALID_SIGNUP_JSON));

        ArgumentCaptor<MemberAgreement> saved = ArgumentCaptor.forClass(MemberAgreement.class);
        verify(memberMapper).insertAgreement(saved.capture());

        // 회원 PK 가 채워지기 전에 저장하면 동의 내역이 주인 없는 행이 된다.
        assertEquals(GENERATED_ID, saved.getValue().getMemberId());
        assertEquals(Boolean.TRUE, saved.getValue().getAgreeCoupleShare());
        // 선택 항목인 마케팅 동의는 null 을 false 로 다룬다.
        assertEquals(Boolean.FALSE, saved.getValue().getAgreeMarketing());
    }

    @Test
    void 필수_약관에_모두_동의하지_않으면_가입할_수_없다() throws Exception {
        ApiException e =
                assertThrows(
                        ApiException.class,
                        () -> authService.signup(request(MISSING_CONSENT_JSON)));

        assertEquals(ErrorCode.CONSENT_REQUIRED, e.getErrorCode());
    }

    @Test
    void 약관_미동의는_DB_를_타기_전에_걸러낸다() throws Exception {
        assertThrows(ApiException.class, () -> authService.signup(request(MISSING_CONSENT_JSON)));

        // 중복 조회조차 하지 않아야 한다 — 거를 수 있는 것은 쿼리 전에 거른다.
        verifyNoInteractions(memberMapper, passwordEncoder, jwtProvider);
    }

    @Test
    void 이미_가입된_이메일이면_가입할_수_없다() throws Exception {
        when(memberMapper.existsByEmail("hong@kb.com")).thenReturn(true);

        ApiException e =
                assertThrows(
                        ApiException.class, () -> authService.signup(request(VALID_SIGNUP_JSON)));

        assertEquals(ErrorCode.EMAIL_EXISTS, e.getErrorCode());
    }

    @Test
    void 이메일이_중복이면_회원을_저장하지_않는다() throws Exception {
        when(memberMapper.existsByEmail("hong@kb.com")).thenReturn(true);

        assertThrows(ApiException.class, () -> authService.signup(request(VALID_SIGNUP_JSON)));

        verify(memberMapper, never()).insert(any());
        verify(memberMapper, never()).insertAgreement(any());
    }

    @Test
    void 회원가입은_온보딩_조회를_하지_않는다() throws Exception {
        givenSignupSucceeds();

        authService.signup(request(VALID_SIGNUP_JSON));

        // 온보딩 진행도는 로그인 응답에만 필요하다. 가입 시 부르면 불필요한 쿼리가 는다.
        verifyNoInteractions(onboardingService);
    }

    // ===== 로그인 =====

    @Test
    void 가입되지_않은_이메일이면_로그인할_수_없다() throws Exception {
        when(memberMapper.findByEmail(EMAIL)).thenReturn(null);

        ApiException e =
                assertThrows(
                        ApiException.class,
                        () -> authService.login(loginRequest(EMAIL, RAW_PASSWORD)));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, e.getErrorCode());
    }

    @Test
    void 비밀번호가_틀리면_로그인할_수_없다() throws Exception {
        Member member = mock(Member.class);
        when(member.getPassword()).thenReturn(ENCODED_PASSWORD);
        when(memberMapper.findByEmail(EMAIL)).thenReturn(member);
        when(passwordEncoder.matches(WRONG_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        ApiException e =
                assertThrows(
                        ApiException.class,
                        () -> authService.login(loginRequest(EMAIL, WRONG_PASSWORD)));

        // 이메일 없음과 같은 에러 코드다 — 어느 쪽이 틀렸는지 알려주면 가입 여부가 노출된다.
        assertEquals(ErrorCode.INVALID_CREDENTIALS, e.getErrorCode());
    }

    @Test
    void 없는_이메일이면_비밀번호_대조까지_가지_않는다() throws Exception {
        when(memberMapper.findByEmail(EMAIL)).thenReturn(null);

        assertThrows(ApiException.class, () -> authService.login(loginRequest(EMAIL, RAW_PASSWORD)));

        // member 가 null 인데 matches 를 부르면 NullPointerException 이 난다. 단축 평가에 기대는 구조다.
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void 로그인에_실패하면_마지막_로그인_시각을_갱신하지_않는다() throws Exception {
        when(memberMapper.findByEmail(EMAIL)).thenReturn(null);

        assertThrows(ApiException.class, () -> authService.login(loginRequest(EMAIL, RAW_PASSWORD)));

        verify(memberMapper, never()).updateLastLoginAt(any());
        verify(jwtProvider, never()).createAccessToken(any());
    }

    @Test
    void 처음_로그인하는_회원은_isFirstLogin_이_참이다() throws Exception {
        // lastLoginAt 을 지정하지 않는다 — 아직 한 번도 로그인하지 않은 회원이다.
        givenLoginSucceeds(mock(Member.class));

        LoginResponse response = authService.login(loginRequest(EMAIL, RAW_PASSWORD));

        assertTrue(response.isFirstLogin());
        assertEquals(ACCESS_TOKEN, response.getAccessToken());
    }

    @Test
    void 로그인한_적이_있는_회원은_isFirstLogin_이_거짓이다() throws Exception {
        Member member = mock(Member.class);
        when(member.getLastLoginAt()).thenReturn(LocalDateTime.of(2026, 8, 1, 9, 0));
        givenLoginSucceeds(member);

        LoginResponse response = authService.login(loginRequest(EMAIL, RAW_PASSWORD));

        assertFalse(response.isFirstLogin());
    }

    @Test
    void 로그인에_성공하면_마지막_로그인_시각을_갱신한다() throws Exception {
        givenLoginSucceeds(mock(Member.class));

        authService.login(loginRequest(EMAIL, RAW_PASSWORD));

        verify(memberMapper).updateLastLoginAt(GENERATED_ID);
    }

    // ===== 로그아웃 =====

    /**
     * 로그아웃은 두 가지를 함께 해야 한다. refresh 만 지우면 남은 access 토큰(최대 1시간)이 계속
     * 통하고, access 만 막으면 refresh 로 새 access 를 받아버린다.
     */
    @Test
    void 로그아웃은_refresh_를_지우고_access_를_폐기한다() {
        when(jwtProvider.getJti(ACCESS_TOKEN)).thenReturn("jti-1");
        when(jwtProvider.getRemainingMs(ACCESS_TOKEN)).thenReturn(60_000L);

        authService.logout(1L, ACCESS_TOKEN);

        verify(refreshTokenStore).delete(1L);
        verify(tokenBlacklist).add("jti-1", 60_000L);
    }

    /** 회원 데이터는 건드리지 않는다. 로그아웃이 프로필을 바꾸거나 토큰을 재발급하면 안 된다. */
    @Test
    void 로그아웃은_회원_데이터를_건드리지_않는다() {
        authService.logout(1L, null);

        verifyNoInteractions(memberMapper, passwordEncoder, onboardingService);
    }

    /**
     * 헤더가 없거나 형식이 맞지 않으면 컨트롤러가 {@code null} 을 넘긴다. 폐기할 대상을 모를 뿐이므로
     * refresh 삭제는 그대로 수행하고 실패시키지 않는다.
     */
    @Test
    void 로그아웃은_access_토큰이_없어도_refresh_를_지운다() {
        assertDoesNotThrow(() -> authService.logout(1L, null));

        verify(refreshTokenStore).delete(1L);
        verifyNoInteractions(tokenBlacklist);
    }

    // ===== 토큰 재발급 =====

    @Test
    void 재발급은_새_access_와_새_refresh_를_함께_돌려준다() {
        when(jwtProvider.getMemberIdFromRefreshToken(REFRESH_TOKEN)).thenReturn(7L);
        when(refreshTokenStore.matches(7L, REFRESH_TOKEN)).thenReturn(true);
        when(jwtProvider.createAccessToken(7L)).thenReturn("new.access");
        when(jwtProvider.createRefreshToken(7L)).thenReturn("new.refresh");

        TokenResponse response = authService.reissue(REFRESH_TOKEN);

        assertEquals("new.access", response.getAccessToken());
        assertEquals("new.refresh", response.getRefreshToken());
    }

    /**
     * 쓴 refresh 토큰은 즉시 새것으로 바뀐다(회전). 저장소에 회원당 하나만 두므로, 새로 저장하는 순간
     * 방금 쓴 토큰은 무효가 된다.
     */
    @Test
    void 재발급하면_새_refresh_토큰이_저장된다() {
        when(jwtProvider.getMemberIdFromRefreshToken(REFRESH_TOKEN)).thenReturn(7L);
        when(refreshTokenStore.matches(7L, REFRESH_TOKEN)).thenReturn(true);
        when(jwtProvider.createRefreshToken(7L)).thenReturn("new.refresh");
        when(jwtProvider.getRefreshTokenValidityMs()).thenReturn(1_209_600_000L);

        authService.reissue(REFRESH_TOKEN);

        verify(refreshTokenStore).save(7L, "new.refresh", 1_209_600_000L);
    }

    /**
     * 서명이 유효해도 서버가 보관 중인 값과 다르면 거절한다. 로그아웃했거나 이미 한 번 재발급에 쓴
     * 토큰이 여기에 걸린다.
     */
    @Test
    void 저장된_것과_다른_refresh_토큰은_거절한다() {
        when(jwtProvider.getMemberIdFromRefreshToken(REFRESH_TOKEN)).thenReturn(7L);
        when(refreshTokenStore.matches(7L, REFRESH_TOKEN)).thenReturn(false);

        ApiException e = assertThrows(ApiException.class, () -> authService.reissue(REFRESH_TOKEN));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, e.getErrorCode());
        verify(jwtProvider, never()).createAccessToken(any());
    }

    // ===== 도우미 =====

    /** 중복 없는 이메일 · 비밀번호 해싱 · PK 채움 · 토큰 발급까지 정상 흐름을 준비한다. */
    private void givenSignupSucceeds() {
        when(memberMapper.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(jwtProvider.createAccessToken(GENERATED_ID)).thenReturn(ACCESS_TOKEN);

        // MyBatis 의 useGeneratedKeys 처럼 insert 시점에 PK 가 채워지는 동작을 흉내낸다.
        doAnswer(
                        invocation -> {
                            invocation.getArgument(0, Member.class).setId(GENERATED_ID);
                            return 1;
                        })
                .when(memberMapper)
                .insert(any(Member.class));
    }

    /** 회원 조회 · 비밀번호 일치 · 토큰 발급까지 로그인 정상 흐름을 준비한다. */
    private void givenLoginSucceeds(Member member) {
        when(member.getId()).thenReturn(GENERATED_ID);
        when(member.getPassword()).thenReturn(ENCODED_PASSWORD);
        when(memberMapper.findByEmail(EMAIL)).thenReturn(member);
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtProvider.createAccessToken(GENERATED_ID)).thenReturn(ACCESS_TOKEN);
    }

    private SignupRequest request(String json) throws Exception {
        return objectMapper.readValue(json, SignupRequest.class);
    }

    private LoginRequest loginRequest(String email, String password) throws Exception {
        return objectMapper.readValue(
                """
                { "email": "%s", "password": "%s" }
                """
                        .formatted(email, password),
                LoginRequest.class);
    }
}
