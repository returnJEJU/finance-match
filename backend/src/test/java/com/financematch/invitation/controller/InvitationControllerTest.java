package com.financematch.invitation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financematch.common.ApiResponse;
import com.financematch.invitation.dto.CommonSurveyResponse;
import com.financematch.invitation.dto.CreateInvitationRequest;
import com.financematch.invitation.dto.CreateInvitationResponse;
import com.financematch.invitation.dto.GetInvitationResponse;
import com.financematch.invitation.dto.UpdateCommonSurveyRequest;
import com.financematch.invitation.service.InvitationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class InvitationControllerTest {

    @Mock private InvitationService invitationService;

    @InjectMocks private InvitationController invitationController;

    @Test
    void createsInvitationAndReturnsCreatedResponse() {
        CreateInvitationRequest request = new CreateInvitationRequest();
        CreateInvitationResponse created = new CreateInvitationResponse("ABCDEFGH");
        when(invitationService.createInvitation(1L, request)).thenReturn(created);

        ResponseEntity<ApiResponse<CreateInvitationResponse>> response =
                invitationController.createInvitation(1L, request);

        verify(invitationService).createInvitation(1L, request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(
                "/api/v1/members/me/invitation",
                response.getHeaders().getLocation().toString());
        assertTrue(response.getBody().isSuccess());
        assertSame(created, response.getBody().getData());
    }

    @Test
    void returnsInvitationForLoginMember() {
        GetInvitationResponse invitation = new GetInvitationResponse(true, "ABCDEFGH");
        when(invitationService.getInvitation(1L)).thenReturn(invitation);

        ApiResponse<GetInvitationResponse> response = invitationController.getInvitation(1L);

        verify(invitationService).getInvitation(1L);
        assertTrue(response.isSuccess());
        assertSame(invitation, response.getData());
    }

    @Test
    void returnsCommonSurveyForLoginMember() {
        CommonSurveyResponse commonSurvey = new CommonSurveyResponse();
        when(invitationService.getCommonSurvey(1L)).thenReturn(commonSurvey);

        ApiResponse<CommonSurveyResponse> response = invitationController.getCommonSurvey(1L);

        verify(invitationService).getCommonSurvey(1L);
        assertTrue(response.isSuccess());
        assertSame(commonSurvey, response.getData());
    }

    @Test
    void updatesCommonSurveyAndReturnsNullData() {
        UpdateCommonSurveyRequest request = new UpdateCommonSurveyRequest();

        ApiResponse<Void> response = invitationController.updateCommonSurvey(1L, request);

        verify(invitationService).updateCommonSurvey(1L, request);
        assertTrue(response.isSuccess());
        assertNull(response.getData());
    }
}
