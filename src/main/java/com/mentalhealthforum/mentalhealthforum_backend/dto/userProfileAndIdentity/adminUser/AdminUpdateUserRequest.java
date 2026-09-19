package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import jakarta.validation.Valid;

/**
 * Request DTO for updating a synced user.
 *
 * <p>All fields are optional:
 * <ul>
 *   <li>{@code groupChange} - If present, changes the user's group</li>
 *   <li>{@code enabledChange} - If present, changes the user's enabled status</li>
 * </ul>
 *
 * <p>If both are present, both changes are applied atomically.
 */
public record AdminUpdateUserRequest(
        @Valid GroupChange groupChange,        // null = no group change
        @Valid EnabledChange enabledChange     // null = no enabled change
) {}
