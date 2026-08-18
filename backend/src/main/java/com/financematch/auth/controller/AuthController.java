package com.financematch.auth.controller;

import com.financematch.auth.annotation.LoginMember;
import com.financematch.auth.dto.LoginRequest;
import com.financematch.auth.dto.LoginResponse;
import com.financematch.auth.dto.SignupRequest;
import com.financematch.auth.dto.SignupResponse;
import com.financematch.auth.dto.TokenReissueRequest;
import com.financematch.auth.dto.TokenResponse;
import com.financematch.auth.service.AuthService;
import com.financematch.common.ApiResponse;
import java.net.URI;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API. 로그인 전에 호출하는 공개 엔드포인트라 {@code /v1/members/me} 아래에 두지 않는다.
 *
 * <p>DispatcherServlet 이 {@code /api/*} 에 매핑돼 있어 클래스 레벨에 {@code /v1} 을 둔다. 실제 주소는
 * {@code /api/v1/auth/signup} 이다.
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    /** 201 응답의 Location — 생성된 리소스는 회원 자신이다. */
    private static final URI MEMBER_LOCATION = URI.create("/api/v1/members/me");

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request) {

        SignupResponse response = authService.signup(request);

        return ResponseEntity.created(MEMBER_LOCATION).body(ApiResponse.ok(response));
    }

    /** 로그인은 새 리소스를 만들지 않으므로 200 이고 Location 도 붙이지 않는다. */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * access 토큰 재발급.
     *
     * <p>인증을 요구하지 않는다. 이 요청은 access 토큰이 만료된 상황에서 호출되므로, 인증을 걸면
     * 재발급 자체가 불가능해진다. 신원 확인은 refresh 토큰이 대신한다.
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> reissue(@Valid @RequestBody TokenReissueRequest request) {
        return ApiResponse.ok(authService.reissue(request.getRefreshToken()));
    }

    /**
     * 로그아웃. 서버에 저장된 refresh 토큰을 지우고, 쓰던 access 토큰을 폐기한다.
     *
     * <p>이 경로는 {@code SecurityConfig} 에서 {@code permitAll} 이지만 인증이 필요하다 — 인증을 요구하는
     * 것은 {@code @LoginMember} 다. 토큰이 없거나 유효하지 않으면 리졸버가 {@code UNAUTHORIZED}(401) 를
     * 던지므로 별도의 검사를 두지 않는다.
     *
     * <p>토큰 문자열을 헤더에서 직접 꺼내 서비스로 넘긴다. 서비스가 {@code HttpServletRequest} 를 알면
     * 안 되므로(계층 규칙), HTTP 를 아는 일은 컨트롤러에서 끝낸다.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @LoginMember Long memberId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        authService.logout(memberId, resolveToken(authorization));
        return ApiResponse.ok();
    }

    /** {@code Authorization: Bearer <token>} 에서 토큰 문자열만 꺼낸다. 형식이 아니면 {@code null}. */
    private String resolveToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
