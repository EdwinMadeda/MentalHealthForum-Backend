package com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.adminUser;

import lombok.Getter;

/**
 * Operation contexts for group dropdowns.
 * Each context represents a specific operation with its own set of allowed groups.
 */
@Getter
public enum GroupContext {
    CREATE("create"),  // Create new user
    REISSUE("reissue"),  // Reissue invitation
    PENDING("pending"), // Update pending invite
    SYNCED("synced"); // Update synced user

    private final String displayName;

    GroupContext(String displayName) {
        this.displayName = displayName;
    }
}
