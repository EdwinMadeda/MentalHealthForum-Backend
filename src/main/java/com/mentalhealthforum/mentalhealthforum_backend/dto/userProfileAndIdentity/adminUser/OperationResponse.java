package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import java.util.List;

/**
 * Generic wrapper for operation responses that include available groups.
 * Used to provide context-aware dropdown options after successful operations.
 *
 * @param <T> The type of the operation result data
 * @param result The actual operation result
 * @param availableGroups Groups available for the next operation
 * @param context The context to interpret the available groups
 * @param guidelines Optional helpful message for the user
 */
public record OperationResponse<T>(
   T result,
   List<AvailableGroup> availableGroups,
   GroupContext context,
   String guidelines
) {}
