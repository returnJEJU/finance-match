package com.financematch.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetInvitationResponse {

    private final boolean hasInvitation;
    private final String inviteCode;
}
