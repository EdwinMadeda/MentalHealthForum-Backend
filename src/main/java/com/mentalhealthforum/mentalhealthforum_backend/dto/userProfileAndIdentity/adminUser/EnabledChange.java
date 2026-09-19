package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Represents an enabled status change request with an optional reason.
 *
 * <p>The {@code isEnabled} field is required when this object is present in the request.
 * At least one reason (template or custom) must be provided.
 */
public record EnabledChange(
    @NotNull(message = "Enabled is required")
    Boolean isEnabled,         // The target status (required)

    UUID reasonDefinitionId,   // Optional reason from definitions

    @Size(max = 500, message = "Custom reason cannot exceed 500 characters")
    String customReason        // Optional custom reason

) { }
