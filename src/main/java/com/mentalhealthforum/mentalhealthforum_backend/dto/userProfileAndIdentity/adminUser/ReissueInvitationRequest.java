package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.validation.ValidEmail;
import com.mentalhealthforum.mentalhealthforum_backend.validation.group.ValidAssignableGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReissueInvitationRequest(
        @NotBlank(message = "Email is required")
        @ValidEmail
        String email,

        @ValidAssignableGroup
        GroupPath group,

        UUID reasonDefinitionId,

        @Size(max = 500, message = "Custom reason cannot exceed 500 characters")
        String customReason,

        boolean sendInvitationEmail

) {}
