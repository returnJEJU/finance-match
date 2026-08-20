package com.financematch.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.auth.dto.LoginRequest;
import com.financematch.auth.dto.LoginResponse;
import com.financematch.auth.dto.SignupRequest;
import com.financematch.auth.dto.SignupResponse;
import com.financematch.auth.dto.TokenReissueRequest;
import com.financematch.auth.dto.TokenResponse;
import com.financematch.auth.service.AuthService;
import com.financematch.common.ApiResponse;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;

    @InjectMocks private AuthController authController;

    // ===== 회원가입 =====

    /**
     * 가입은 회원이라는 리소스를 새로 만드는 요청이므로 200 이 아니라 201 이다. 생성된 리소스의 위치도
     * 함께 알려준다 — 회원 식별자를 경로에 노출하지 않는 규칙에 따라 {@code /api/v1/members/me} 다.
     */
    @Test
    void 회원가입_성공_응답은_201_과_Location_을_함께_돌려준다() {
        SignupRequest request = mock(SignupRequest.class);
        when(authService.signup(request)).thenReturn(mock(SignupResponse.class));

        ResponseEntity<ApiResponse<SignupResponse>> response = authController.signup(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(URI.create("/api/v1/members/me"), response.getHeaders().getLocation());
    }

    @Test
    void 회원가입_응답은_서비스_결과를_그대로_감싼다() {
        SignupRequest request = mock(SignupRequest.class);
        SignupResponse serviceResult = mock(SignupResponse.class);
        when(authService.signup(request)).thenReturn(serviceResult);

        ResponseEntity<ApiResponse<SignupResponse>> response = authController.signup(request);

        assertTrue(response.getBody().isSuccess());
        // 컨트롤러가 응답을 재가공하지 않는지 — 조립은 서비스가 끝낸다.
        assertSame(serviceResult, response.getBody().getData());
    }

    // ===== 로그인 =====

    /** 로그인은 새 리소스를 만들지 않으므로 201·Location 이 아니라 평범한 200 이다. */
    @Test
    void 로그인_응답은_서비스_결과를_그대로_감싼다() {
        LoginRequest request = mock(LoginRequest.class);
        LoginResponse serviceResult = mock(LoginResponse.class);
        when(authService.login(request)).thenReturn(serviceResult);

        ApiResponse<LoginResponse> response = authController.login(request);

        assertTrue(response.isSuccess());
        assertSame(serviceResult, response.getData());
        assertNull(response.getCode());
    }

    @Test
    void 로그인은_요청을_그대로_서비스에_넘긴다() {
        LoginRequest request = mock(LoginRequest.class);
        when(authService.login(request)).thenReturn(mock(LoginResponse.class));

        authController.login(request);

        verify(authService).login(request);
    }

    // ===== 재발급 =====

    /**
     * 재발급은 access 토큰이 만료된 상황에서 호출되므로 인증을 걸 수 없다. 신원 확인은 요청 본문의
     * refresh 토큰이 대신하고, 컨트롤러는 그 값을 서비스로 넘기기만 한다.
     */
    @Test
    void 재발급은_본문의_refresh_토큰을_서비스에_넘긴다() {
        TokenReissueRequest request = mock(TokenReissueRequest.class);
        when(request.getRefreshToken()).thenReturn("saved.refresh.token");
        when(authService.reissue("saved.refresh.token")).thenReturn(mock(TokenResponse.class));

        authController.reissue(request);

        verify(authService).reissue("saved.refresh.token");
    }

    /** 새 리소스를 만드는 것이 아니라 토큰을 바꿔주는 요청이라 201 이 아닌 200 이다. */
    @Test
    void 재발급_응답은_서비스_결과를_그대로_감싼다() {
        TokenReissueRequest request = mock(TokenReissueRequest.class);
        TokenResponse serviceResult = mock(TokenResponse.class);
        when(request.getRefreshToken()).thenReturn("saved.refresh.token");
        when(authService.reissue("saved.refresh.token")).thenReturn(serviceResult);

        ApiResponse<TokenResponse> response = authController.reissue(request);

        assertTrue(response.isSuccess());
        assertSame(serviceResult, response.getData());
        assertNull(response.getCode());
    }

    // ===== 로그아웃 =====

    @Test
    void 로그아웃은_회원_ID_와_토큰_문자열을_서비스에_넘긴다() {
        authController.logout(1L, "Bearer abc.def.ghi");

        verify(authService).logout(1L, "abc.def.ghi");
    }

    /**
     * 서비스는 HTTP 를 몰라야 하므로 {@code Bearer } 를 떼는 일은 컨트롤러가 한다. 헤더가 없거나
     * 형식이 다르면 {@code null} 을 넘긴다 — 폐기할 토큰을 모를 뿐이라 로그아웃 자체는 진행된다.
     */
    @Test
    void 로그아웃은_Authorization_헤더가_없으면_null_을_넘긴다() {
        authController.logout(1L, null);

        verify(authService).logout(1L, null);
    }

    @Test
    void 로그아웃은_Bearer_형식이_아니면_null_을_넘긴다() {
        authController.logout(1L, "abc.def.ghi");

        verify(authService).logout(1L, null);
    }

    @Test
    void 로그아웃_성공_응답에는_데이터가_없다() {
        ApiResponse<Void> response = authController.logout(1L, "Bearer abc.def.ghi");

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        assertNull(response.getCode());
    }

    /**
     * 명세 예시에는 "로그아웃되었습니다." 가 있지만 서버는 채우지 않는다. 프론트의 응답 인터셉터가 성공 시
     * data 만 돌려주고 message 를 버리는 구조라 담아도 도달하지 않는다 — 안내 문구는 프론트가 표시한다.
     */
    @Test
    void 로그아웃_성공_응답에는_message_를_담지_않는다() {
        ApiResponse<Void> response = authController.logout(1L, "Bearer abc.def.ghi");

        assertNull(response.getMessage());
    }
}
