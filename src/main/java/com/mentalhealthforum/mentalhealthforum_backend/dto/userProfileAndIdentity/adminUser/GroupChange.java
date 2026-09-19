package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.validation.group.ValidAssignableGroup;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Represents a group change request with an optional reason.
 *
 * <p>The {@code group} field is required when this object is present in the request.
 * At least one reason (template or custom) must be provided.
 */
public record GroupChange(
    @NotNull(message = "Group assignment is required.")
    @ValidAssignableGroup
    GroupPath group,           // The target group (required)

    UUID reasonDefinitionId,   // Optional reason from definitions

    @Size(max = 500, message = "Custom reason cannot exceed 500 characters")
    String customReason        // Optional custom reason
) {}
