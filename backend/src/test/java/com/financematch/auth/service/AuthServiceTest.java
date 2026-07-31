package com.financematch.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import com.financematch.auth.jwt.JwtProvider;
import com.financematch.auth.mapper.MemberMapper;
import com.financematch.onboarding.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private MemberMapper memberMapper;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private JwtProvider jwtProvider;

    @Mock private OnboardingService onboardingService;

    @InjectMocks private AuthService authService;

    /**
     * JWT 는 서버가 로그인 상태를 들고 있지 않아 로그아웃 시 지울 것이 없다. 회원 정보를 바꾸거나 토큰을 다시
     * 발급하는 동작이 끼어들면 안 된다 — Redis 블랙리스트를 얹기 전까지 이 메서드는 기록만 남긴다.
     */
    @Test
    void 로그아웃은_회원_데이터를_건드리지_않는다() {
        authService.logout(1L);

        verifyNoInteractions(memberMapper, passwordEncoder, jwtProvider, onboardingService);
    }

    @Test
    void 로그아웃은_회원_ID_만으로_끝난다() {
        assertDoesNotThrow(() -> authService.logout(1L));
    }
}
