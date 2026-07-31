package com.financematch.auth.controller;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.auth.dto.WithdrawRequest;
import com.financematch.auth.dto.WithdrawResponse;
import com.financematch.auth.service.MemberService;
import com.financematch.common.ApiResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private MemberService memberService;

    @InjectMocks private MemberController memberController;

    @Test
    void 탈퇴_결과를_그대로_감싸_반환한다() throws Exception {
        WithdrawRequest request = request();
        WithdrawResponse withdrawResponse =
                WithdrawResponse.of(1L, LocalDateTime.of(2026, 7, 31, 10, 0, 0));
        when(memberService.withdraw(1L, request)).thenReturn(withdrawResponse);

        ApiResponse<WithdrawResponse> response = memberController.withdraw(1L, request);

        verify(memberService).withdraw(1L, request);
        assertTrue(response.isSuccess());
        assertSame(withdrawResponse, response.getData());
    }

    /** 성공 응답의 안내 문구는 프론트가 표시한다. 서버는 message 를 채우지 않는다. */
    @Test
    void 성공_응답에는_message_를_담지_않는다() throws Exception {
        WithdrawRequest request = request();
        when(memberService.withdraw(1L, request))
                .thenReturn(WithdrawResponse.of(1L, LocalDateTime.of(2026, 7, 31, 10, 0, 0)));

        ApiResponse<WithdrawResponse> response = memberController.withdraw(1L, request);

        assertNull(response.getMessage());
        assertNull(response.getCode());
    }

    private WithdrawRequest request() throws Exception {
        return objectMapper.readValue(
                """
                { "password": "Pw123456!", "confirmationText": "회원 탈퇴" }
                """,
                WithdrawRequest.class);
    }
}
