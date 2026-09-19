package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for revoking an admin invitation.
 *
 * <p>A reason is required (template or custom) for revocation.
 */
public record RevokeInvitationRequest(
        UUID reasonDefinitionId,

        @Size(max = 500, message = "Custom reason cannot exceed 500 characters")
        String customReason
) {}
