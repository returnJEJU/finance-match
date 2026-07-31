package com.financematch.auth.controller;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.financematch.auth.service.AuthService;
import com.financematch.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;

    @InjectMocks private AuthController authController;

    @Test
    void 로그아웃은_인증된_회원_ID_를_서비스에_넘긴다() {
        authController.logout(1L);

        verify(authService).logout(1L);
    }

    @Test
    void 로그아웃_성공_응답에는_데이터가_없다() {
        ApiResponse<Void> response = authController.logout(1L);

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
        ApiResponse<Void> response = authController.logout(1L);

        assertNull(response.getMessage());
    }
}
