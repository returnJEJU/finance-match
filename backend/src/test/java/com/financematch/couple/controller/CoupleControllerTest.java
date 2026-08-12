package com.financematch.couple.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.common.ApiResponse;
import com.financematch.couple.dto.CoupleProfileMessageRequest;
import com.financematch.couple.dto.CoupleProfileMessageResponse;
import com.financematch.couple.dto.CreateCoupleRequest;
import com.financematch.couple.dto.CreateCoupleResponse;
import com.financematch.couple.service.CoupleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CoupleControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private CoupleService coupleService;

    @InjectMocks private CoupleController coupleController;

    /** 커플 생성 결과를 201 상태와 Location 헤더를 포함한 성공 응답으로 반환하는지 검증한다. */
    @Test
    void createsCoupleAndReturnsCreatedResponse() throws Exception {
        CreateCoupleRequest request = createCoupleRequest();
        CreateCoupleResponse created = new CreateCoupleResponse("김하나");
        when(coupleService.createCouple(2L, request)).thenReturn(created);

        ResponseEntity<ApiResponse<CreateCoupleResponse>> response =
                coupleController.createCouple(2L, request);

        verify(coupleService).createCouple(2L, request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(
                "/api/v1/members/me/couple",
                response.getHeaders().getLocation().toString());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertSame(created, response.getBody().getData());
    }

    /** 커플 해제 서비스를 호출하고 데이터가 없는 성공 응답을 반환하는지 검증한다. */
    @Test
    void disconnectsCoupleAndReturnsNullData() {
        ApiResponse<Void> response = coupleController.disconnectCouple(1L);

        verify(coupleService).disconnectCouple(1L);
        assertTrue(response.isSuccess());
        assertNull(response.getData());
    }

    /** 인증된 회원의 커플 한줄 소개를 성공 응답에 담아 반환하는지 검증한다. */
    @Test
    void returnsProfileMessageForLoginMember() {
        CoupleProfileMessageResponse profile =
                new CoupleProfileMessageResponse("김하나", "이두리", "함께 부자 되기");
        when(coupleService.getProfileMessage(1L)).thenReturn(profile);

        ApiResponse<CoupleProfileMessageResponse> response =
                coupleController.getProfileMessage(1L);

        verify(coupleService).getProfileMessage(1L);
        assertTrue(response.isSuccess());
        assertSame(profile, response.getData());
    }

    /** 요청받은 한줄 소개를 서비스에 전달하고 수정 결과를 반환하는지 검증한다. */
    @Test
    void updatesProfileMessageAndReturnsUpdatedProfile() throws Exception {
        CoupleProfileMessageRequest request = profileMessageRequest();
        CoupleProfileMessageResponse updated =
                new CoupleProfileMessageResponse("김하나", "이두리", "함께 부자 되기");
        when(coupleService.updateProfileMessage(1L, "함께 부자 되기"))
                .thenReturn(updated);

        ApiResponse<CoupleProfileMessageResponse> response =
                coupleController.updateProfileMessage(1L, request);

        verify(coupleService).updateProfileMessage(1L, "함께 부자 되기");
        assertTrue(response.isSuccess());
        assertSame(updated, response.getData());
    }

    private CreateCoupleRequest createCoupleRequest() throws Exception {
        return objectMapper.readValue(
                """
                { "inviteCode": "ABCDEFGH" }
                """,
                CreateCoupleRequest.class);
    }

    private CoupleProfileMessageRequest profileMessageRequest() throws Exception {
        return objectMapper.readValue(
                """
                { "profileMessage": "함께 부자 되기" }
                """,
                CoupleProfileMessageRequest.class);
    }
}
