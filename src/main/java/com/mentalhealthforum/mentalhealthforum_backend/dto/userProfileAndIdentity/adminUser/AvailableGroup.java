package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import com.mentalhealthforum.mentalhealthforum_backend.enums.GroupPath;

/**
 * Represents a group available for assignment in dropdowns.
 *
 * @param value The enum name (e.g., "MEMBERS_NEW")
 * @param path The group path (e.g., "/members/new")
 * @param displayName The human-readable name (e.g., "New members")
 * @param isCurrent Whether this is the user's current group
 * @param action PROMOTE, DEMOTE, CORRECT, SAME
 * @param warningMessage Optional warning message (null if no warning)
 */
public record AvailableGroup(
        String value,
        String path,
        String displayName,
        boolean isCurrent,
        ActionType action,
        String warningMessage
) {
    public AvailableGroup(GroupPath groupPath){
        this(groupPath, false);
    }

    public AvailableGroup(GroupPath groupPath, boolean isCurrent){
        this (
                groupPath.name(),
                groupPath.getPath(),
                groupPath.getDisplayName(),
                isCurrent,
                ActionType.SAME,
                null
        );
    }

    public AvailableGroup(GroupPath groupPath, boolean isCurrent, ActionType action, String warningMessage){
        this (
                groupPath.name(),
                groupPath.getPath(),
                groupPath.getDisplayName(),
                isCurrent,
                action,
                warningMessage
        );
    }

}
