package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import jakarta.validation.Valid;

/**
 * Request DTO for updating a pending admin invitation.
 *
 * <p>All fields are optional:
 * <ul>
 *   <li>{@code groupChange} - If present, changes the pending user's group</li>
 *   <li>{@code enabledChange} - If present, changes the pending user's enabled status</li>
 * </ul>
 *
 * <p>If both are present, both changes are applied atomically.
 */
public record UpdatePendingAdminInviteRequest(
        @Valid GroupChange groupChange,        // null = no group change
        @Valid EnabledChange enabledChange     // null = no enabled change
) {}
