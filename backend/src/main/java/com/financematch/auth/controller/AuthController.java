package com.financematch.auth.controller;

import com.financematch.auth.dto.LoginRequest;
import com.financematch.auth.dto.LoginResponse;
import com.financematch.auth.dto.SignupRequest;
import com.financematch.auth.dto.SignupResponse;
import com.financematch.auth.service.AuthService;
import com.financematch.common.ApiResponse;
import java.net.URI;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
}
