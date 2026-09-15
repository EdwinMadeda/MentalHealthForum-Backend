package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;
import com.mentalhealthforum.mentalhealthforum_backend.enums.UserType;

import java.util.List;

/**
 * Generic wrapper for admin user details, providing:
 * <ul>
 *   <li>The wrapped result (UserResponse or PendingAdminInviteDto)</li>
 *   <li>Current group (convenience field)</li>
 *   <li>Available groups for assignment</li>
 *   <li>Full audit history</li>
 *   <li>Next-step guidelines</li>
 * </ul>
 *
 * <p>The {@code type} discriminator indicates whether the result is:
 * <ul>
 *   <li>{@code SYNCED} - A fully onboarded user from {@code app_users}</li>
 *   <li>{@code PENDING} - A pending user from {@code admin_invitations}</li>
 * </ul>
 *
 * @param <T> The type of the wrapped result (UserResponse or PendingAdminInviteDto)
 */
public record AdminUserDetailsDto<T>(
        UserType type,                          // SYNCED or PENDING
        T result,                               // UserResponse OR PendingAdminInviteDto
        GroupPath currentGroup,                 // Current primary group
        List<AvailableGroup> availableGroups,   // Context-aware dropdown options
        List<UserHistoryEntry> recentHistory,   // Recent history
        String guidelines                       // Next-step guidance                     // Next-step guidance
) {}
